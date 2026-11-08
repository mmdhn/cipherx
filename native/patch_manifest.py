import sys
p = sys.argv[1]
s = open(p, encoding='utf8').read()
if 'cipherx.OPEN' in s:
    print('manifest already patched'); sys.exit(0)
flt = '''
            <!-- cipherx.OPEN -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <category android:name="android.intent.category.BROWSABLE"/>
                <data android:scheme="content"/>
                <data android:scheme="file"/>
                <data android:mimeType="application/octet-stream"/>
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.SEND"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <data android:mimeType="*/*"/>
            </intent-filter>
        '''
i = s.index('</activity>')
open(p, 'w', encoding='utf8').write(s[:i] + flt + s[i:])
print('manifest patched')
