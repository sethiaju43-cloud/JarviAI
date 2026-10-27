# Jarvi AI — shuru yahan se karein (Hindi)

Bhai, yeh GitHub par upload karne ke liye project folder hai.

## Zaroori sach
- Yeh ZIP aapki bheji hui `JarvisApp-main.zip` ke project par based hai; ise naya naam diya gaya hai aur yeh Hindi guide jodi gayi hai.
- Is package mein natural voice interruption ya anya naye features ko abhi independently implement/test karne ka daava nahi hai.
- Original README ke mutabik build aur tests pehle verify nahi kiye gaye the. GitHub Actions green hone aur APK phone par test hone tak ise tested na maanein.
- Upload se pehle project ko GitHub par private repository mein rakhna behtar hai agar aap ise abhi public nahi karna chahte.

## Android phone se GitHub par upload
1. GitHub app ya browser mein sign in karein.
2. `+` dabayein aur **New repository** chunein.
3. Repository ka naam `JarviAI` rakhein. Shuru mein **Private** rakh sakte hain.
4. Repository banayein.
5. Is ZIP ko phone mein extract karein. Extract hone ke baad `JarviAI` folder ke andar ki files upload honi chahiye — ek aur extra `JarviAI/JarviAI` folder na ban jaaye.
6. Agar GitHub mobile page par folder upload mushkil ho, computer ya GitHub Desktop/Android-compatible Git client ka istemal karein. ZIP file ko seedha repository mein rakh dene se source files apne aap repository ki tarah nahi khulti.
7. Upload ke baad repository ke **Actions** tab mein build workflow ka result dekhein.

## APK banane ka agla kadam
- GitHub Actions mein build workflow chale to uska status dekhein.
- Agar build fail ho, Actions ka pehla laal error log copy karke developer/AI ko bhejein.
- Agar build pass ho, workflow ke Artifacts section se APK artifact download karein, agar workflow ne artifact publish kiya ho.
- APK install karke phone par voice, Hindi/Hinglish, memory, permissions aur offline model ko alag-alag test karein.

## AI se code badalwate waqt
- Har badlav se pehle repository mein commit/save karein, taaki purana code wapas laa sakein.
- AI se ek waqt mein ek file badalwayein; usse exact file path aur poora replacement code maangein.
- Purani file delete karne se pehle backup/commit karein.
- API keys, passwords, OTP ya private tokens GitHub par upload na karein.

## Abhi ke project mein mukhya kami
Original README ke hisaab se voice conversation mode suno → samjho → bolo → phir suno cycle hai; yeh asli real-time interruption nahi. Local model ko bolte waqt rokne ka support bhi pending bataya gaya hai. Isko agle development phase mein implement aur test karna hoga.
