# Stay Awake 1.2

Native Android helper installed as `in.room.stayawake`.

## Silent video in picture-in-picture

Tap **Start silent video in PiP**. A bundled four-second silent video loops in a small movable window. You can open MirAIe or another app behind it. Close the PiP window to stop playback. It uses Android's normal video playback and keep-screen-on behavior. It needs no internet or settings-changing permission. The video stops when its activity is no longer visible.

## Foreground service

**Keep screen awake** starts a foreground service with a screen-bright wake lock. **Restore normal sleep** releases it. Optionally allow notifications to see its Stop action, or add its Quick Settings tile. The power button can still lock the phone. The wake-lock API is deprecated but supported on the test phone; long-term manufacturer behavior is not guaranteed.

## Device policy and tests

The connected Android 16 phone reports an enforced device-admin maximum timeout of 300,000 ms and a restriction on configuring screen timeout. The initial timeout-changing approach was abandoned because the phone restored 15,000 ms after leaving the app. The final build requests neither WRITE_SETTINGS nor WRITE_SECURE_SETTINGS, and does not change screen timeout or organization policies.

Foreground-service test: Android reported the service as foreground and its screen-bright wake lock held after more than 25 seconds, with screen awake outside the app. This was not a five-minute validation.

PiP test: Android reported the VideoActivity in pinned mode, a local looping media player actively rendering, and the video window holding the screen awake. The video kept the phone awake with MirAIe open for more than five minutes while the reported organization policy remained enforced. Restore normal sleep was then verified to remove the PiP hold-screen window, and PiP was restarted.

The temporary USB Stay Awake setting used during investigation was restored to its original value, 0. System screen timeout remains its original 15,000 ms.

Build with `python3 build.py` using the installed Android SDK and OpenJDK. Output: `stay-awake.apk`. Development-signed for this device; no network, accessibility, device-admin, overlay, or secure-settings permission.
