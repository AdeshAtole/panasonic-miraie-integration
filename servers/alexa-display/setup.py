#!/usr/bin/env python3
"""Explicit owner-run setup. Never invoked by build/test. No appliance writes."""
import argparse
import getpass
import json
from pathlib import Path
import sys
import time

sys.path.insert(0, str(Path(__file__).parent / 'src'))
from protocol import AC, BASE, Fault, login, request
from skill import Journal
from owner_login import login_owner


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--region', default='eu-west-1')
    parser.add_argument('--secret', required=True, help='Dedicated Secrets Manager secret name or ARN')
    sub = parser.add_subparsers(dest='action', required=True)
    sub.add_parser('connect', help='Sign in, select AC and create a dedicated cloud session')
    sub.add_parser('reconnect', help='Replace only the MirAIe session; keep owner/device binding')
    reconcile = sub.add_parser('reconcile', help='Read uncertain command state; never resend')
    reconcile.add_argument('--table', required=True)
    reconcile.add_argument('--clear-uncertain', action='store_true', help='Explicitly acknowledge the result and allow future voice commands')
    args = parser.parse_args()
    import boto3
    secrets = boto3.client('secretsmanager', region_name=args.region)

    def save(config):
        secrets.put_secret_value(SecretId=args.secret, SecretString=json.dumps(config))

    if args.action in ('connect', 'reconnect'):
        # Both fields are hidden. Password is used once and never stored.
        account = getpass.getpass('MirAIe email or mobile (hidden): ')
        password = getpass.getpass('MirAIe password (hidden): ')
        credentials = login(account, password)
        del password, account
        if args.action == 'reconnect':
            config = json.loads(secrets.get_secret_value(SecretId=args.secret)['SecretString'])
            config['session'] = credentials
            # Ensure selected device still belongs to the newly authenticated account.
            homes = request(BASE + 'homeManagement/homes', token=credentials['accessToken'], timeout=15)
            ids = [d.get('deviceId') for h in homes for s in h.get('spaces', []) for d in s.get('devices', [])]
            if config['device']['deviceId'] not in ids:
                raise Fault('INVALID_VALUE')
            AC(config, lambda _: None).prepare()
            save(config)
            print('Dedicated cloud session replaced. No appliance command sent.')
            return
        homes = request(BASE + 'homeManagement/homes', token=credentials['accessToken'], timeout=15)
        devices = [d for h in homes for s in h.get('spaces', []) for d in s.get('devices', [])
                   if d.get('category') == 'AC' and str(d.get('modelId')) == '140013'
                   and not d.get('thirdPartyService') and not d.get('parentDeviceId')]
        if not devices:
            raise Fault('INVALID_VALUE')
        for index, device in enumerate(devices, 1):
            print(f"{index}. {device.get('deviceName', 'AC')} (reviewed model 140013)")
        chosen = int(input('Select device number explicitly: ')) - 1
        if not 0 <= chosen < len(devices):
            raise Fault('INVALID_VALUE')
        selected = devices[chosen]
        device = {k: selected[k] for k in ('deviceId', 'modelId', 'category')}
        client = getpass.getpass('Login with Amazon client ID (hidden): ')
        owner_id = login_owner(client)
        config = {'session': credentials, 'device': device, 'lwa_client_id': client,
                  'owner_id': owner_id, 'friendly_name': 'AC display'}
        AC(config, lambda _: None).prepare()
        # connect intentionally creates only: never overwrite an existing binding silently.
        secrets.create_secret(Name=args.secret, SecretString=json.dumps(config))
        print('Private configuration stored. No password or LWA access token saved. No appliance command sent.')
    else:
        config = json.loads(secrets.get_secret_value(SecretId=args.secret)['SecretString'])
        journal = Journal(boto3.resource('dynamodb', region_name=args.region).Table(args.table))
        pending = journal.blocked()
        if not pending:
            print('No uncertain command is blocking the integration.')
            return
        if time.time() - int(pending['created']) < 30:
            raise Fault('INTERNAL_ERROR')  # A Lambda invocation could still be finishing.
        state = AC(config, save).prepare()
        print('Requested display:', pending['target'], '| Cloud-reported display:', state['acdc'])
        print('This is cloud readback, not physical confirmation. No command resent.')
        if args.clear_uncertain:
            journal.unblock(pending['request'])
            print('Uncertainty acknowledged. Future NEW voice requests are enabled; old requests remain deduplicated.')


if __name__ == '__main__':
    try:
        main()
    except (Exception, KeyboardInterrupt):
        print('Setup did not finish. Check the supplied account, AWS permissions and connection. No automatic retry.', file=sys.stderr)
        sys.exit(1)
