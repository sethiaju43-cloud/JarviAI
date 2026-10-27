# Jarvi AI — agle sudhaar ke liye checklist

Yeh checklist hai, implementation ka proof nahi.

- [ ] Pehle existing code ka audit aur baseline build/test
- [ ] Natural conversation aur conversation context ko verify karna
- [ ] Respectful logical debate aur uncertainty handling ko test karna
- [ ] Voice interruption: user bolna shuru kare to TTS/old request cancel ho
- [ ] Hands-free listening loop: duplicate requests aur microphone leaks na hon
- [ ] Hindi/Hinglish speech recognition aur TTS ka phone par test
- [ ] Memory: current chat, preferences, relevant past memories; view/edit/delete controls
- [ ] Online/offline model fallback aur provider errors ko test karna
- [ ] Phone actions ke liye confirmation aur Android permissions verify karna
- [ ] GitHub Actions build pass; tests ke actual results record karna
- [ ] APK install karke physical phone par regression test

## AI coding agent ke liye nirdesh
Project ki files ko pehle padho. Ek baar mein poora project rewrite mat karo. Pehle build/test baseline banao. Phir chhote, reviewable changes karo. Har change ke baad tests chalao. Jo test nahi chala, use unverified likho. API keys ya secrets ko source code mein mat rakho.
