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
