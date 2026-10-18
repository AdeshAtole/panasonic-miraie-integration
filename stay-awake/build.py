from pathlib import Path
import subprocess, os
root=Path(__file__).resolve().parent
sdk=Path.home()/'Library/Android/sdk'
bt=sdk/'build-tools/35.0.0'
jdk=Path('/opt/homebrew/opt/openjdk/bin')
android=sdk/'platforms/android-35/android.jar'
b=root/'build'; b.mkdir(exist_ok=True)
(b/'classes').mkdir(exist_ok=True);(b/'generated').mkdir(exist_ok=True)
env=dict(os.environ,JAVA_HOME=str(jdk.parent),PATH=str(jdk)+':'+os.environ['PATH'])
def run(*args):subprocess.run([str(x) for x in args],check=True,cwd=root,env=env)
run(bt/'aapt','package','-f','-m','-J',b/'generated','-M',root/'AndroidManifest.xml','-S',root/'res','-I',android,'-F',b/'resources.apk')
run(jdk/'javac','--release','8','-classpath',android,'-d',b/'classes',*root.glob('src/**/*.java'),*(b/'generated').glob('**/*.java'))
run(bt/'d8','--lib',android,'--min-api','26','--output',b,*(b/'classes').glob('**/*.class'))
import zipfile, shutil
shutil.copy(b/'resources.apk',b/'unsigned.apk')
with zipfile.ZipFile(b/'unsigned.apk','a') as z:z.write(b/'classes.dex','classes.dex')
key=b/'development.p12'
if not key.exists():run(jdk/'keytool','-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','development','-dname','CN=Room Development','-keyalg','RSA','-validity','3650')
run(bt/'zipalign','-f','4',b/'unsigned.apk',b/'aligned.apk')
run(bt/'apksigner','sign','--ks',key,'--ks-key-alias','development','--ks-pass','pass:android','--out',root/'stay-awake.apk',b/'aligned.apk')
run(bt/'apksigner','verify',root/'stay-awake.apk')
