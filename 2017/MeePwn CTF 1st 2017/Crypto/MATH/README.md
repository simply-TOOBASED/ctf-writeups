>I hack your brain! <br/> <br/> [hack.py](./hack.py)

Looking at [hack.py](./hack.py),
```
from Crypto.Util.number import *
from hashlib import md5

flag = "XXX"
assert len(flag) == 14
pad = bytes_to_long(md5(flag).digest())

hack = 0

for char in flag:
	hack+= pad
	hack*= ord(char)
	
print hack
#hack = 64364485357060434848865708402537097493512746702748009007197338675
#flag_to_submit = "MeePwnCTF{" + flag + "}"
```
We first try to factor the `hack` number: http://factordb.com/index.php?query=64364485357060434848865708402537097493512746702748009007197338675, so we see that it is factorable. The reason why we do this is because we know that `hack` is multiplied by the last character of the flag before its value is `64364485357060434848865708402537097493512746702748009007197338675`, so the last character of the flag is one of the factors of `64364485357060434848865708402537097493512746702748009007197338675`. 

Assuming that the flag is printable ASCII characters, we don't have to search all 255 possible chars, just letters (upper and lowercase), numbers, and punctuation. Looking at the divisors of `hack`, we conclude that the possible values of our flag is `[105, 107, 75, 35, 45, 63]` or `['i', 'k', 'K', '#', '-', '?']`. 
