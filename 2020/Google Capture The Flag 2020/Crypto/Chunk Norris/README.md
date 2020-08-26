# Challenge
Chunk Norris is black belt in fast random number generation.

We are given 2 files, a [challenge.py](challenge.py) and [output.txt](output.txt) file. It looks like the flag was encrypted with textbook RSA, and that the 2 primes were the modulus (both 1024-bit) were generated in an unsafe way.

The goal is to recover the primes and decrypt the ciphertext to get the flag.

## Weak Prime Generation

Let's a take look at the prime generatation and see why it's insecure.
```python
#!/usr/bin/python3 -u

import random
from Crypto.Util.number import *
import gmpy2

a = 0xe64a5f84e2762be5
chunk_size = 64

def gen_prime(bits):
  s = random.getrandbits(chunk_size)

  while True:
    s |= 0xc000000000000001
    p = 0
    for _ in range(bits // chunk_size):
      p = (p << chunk_size) + s
      s = a * s % 2**chunk_size
    if gmpy2.is_prime(p):
      return p

n = gen_prime(1024) * gen_prime(1024)
e = 65537
flag = open("flag.txt", "rb").read()
print('n =', hex(n))
print('e =', hex(e))
print('c =', hex(pow(bytes_to_long(flag), e, n)))
```

If we look closely at the `gen_prime` function, we can see a couple of things.

Every prime generated will have 3 bits always set, bit 1023, 1022, and 1. This means that every prime will be in the form:
```
p = 2^1023 + 2^1022 + 1 + ...
```
`chunk_size` is set to 64. With the way the prime is generated, we're taking 1024 bits, spliting it into 64-bit chunks (hence the name) and putting a 64-bit number in there. What this means is that are primes actually look like this:
```
p = (c1 * 2^960) + (c2 * 2^896) + (c3 * 2^832) + ... + c16
```
Where `c1, c2, ..., c16` are each individiual 64-bit chunk.

Essentially it's 16 different 64-bit numbers all merged together into 1 1024-bit number. The good news is, with the first and last chunk we can easily recover the s values and then regenerate the primes that were used. Let's take a look at the first chunk and how it's generated.
## Determining each chunk value and recovering s
```python
s = random.getrandbits(64)
p = 0
p = (p << 64) + s = s
s = (a * s) % 2**64
```
So the first chunk is basically `s`. Every loop, `s` is updated by multiplying it with `a` and taking the value `mod 2**64`. Then this value is used in the next chunk. What this tells us is that the chunks have the form. 
```
ck = (a^(k-1)*s) mod 2**64
c1 = s
c2 = (a*s) mod 2**64
c3 = (a * ((a*s) mod 2**64)) mod 2**64 = (a^2*s) mod 2**64
...
c16 = (a^15*s) mod 2**64
```
We only need to take the `mod 2**64` once, as doing it multiple times will arrive at the same result. With this new knowledge, we can easily recover s. But there's an extra step; this is the form for one of the primes, but `n` is the product of 2 of these primes! So we have to apply a few math tricks to figure out `s`.
```
N = p*q
N = (c1_p*2^960 + c2_p*2^896 + c2_p*2^832 + ... c16_p) * (c1_q*2^960 + c2_q*2^896 + c2_q*2^832 + ... c16_q)
N = (c1_p*c1_q*2^1920 + (c1_p*c2_q+c2_p*c1_q)*2^1856 + ... + c16_p*c_16_q)
```
We know that `N` is 2048 bits, and that any product of the 2 chunks is 128 bits. Thus, we know that `c1_p*c1_q*2^1920` is the upper 128 bits of N. However, the lower 64-bits of those 128-bits is contributed by the upper 64-bits of `(c1_p*c2_q+c2_p*c1_q)*2^1856`. This means that the 64 MSB bits of N is equivalent to the upper 64-bits of `c1_p*c1_q`.

We said before that the first chunk is essentially `s`. This means that `c1_p*c1_q` is equivalent to `s_p*s_q`. So we can do
```
upper_64_bits_of_n = (n >> 1984) = (s_p*s_q) >> 64 = upper_64_bits_of_sp_sq`
```
We can't do much with just the upper 64 bits of the products of the s values, we need to figure out the lower 64 bits as well. We can do that with the last product in our expansion of `N`.

Just how we used the upper 64 bits of `N`, we know that the lower 64 bits of N are contributed only by the lower 64 bits of `c16_p*c_16_q`.
```
c16_p*c_16_q = ((a^15*s_p) mod 2**64) * ((a^15*s_q) mod 2**64) = (a^30*s_p*s_q) mod 2**64
```
Because we know the value of `a`, we can easily figure out the lower 64 bits of `s_p*s_q`.
```
import gmpy2
a^30 mod 2**64 = pow(0xe64a5f84e2762be5, 30, 2**64) = 13212876016237202665
s_p*s_q % 2**64 = ((n % 2**64) * gmpy2.invert(13212876016237202665, 2**64)) % 2**64 = lower_64_bits_of_sp_sq
```
We essentially do an inverse_mod with `a^30` and `2**64` and multiply this by the lower 64-bits of `n` to figure out the lower 64-bits of the product of the 2 s values. We can combine this with the upper 64-bits we recovered and get the entire 128-bits. 

Because the number is only 128-bits, we can easily factor it with the sympy library, and we can use the fact that we know that some of the bits (1023rd, 1022nd, and 1st) are set in the prime to figure out the correct prime from the candidates we have.

There is **ONE** caveat. When testing, it was discovered that upper 64 bits we recover is actually the upper 64 bits of the products +/- a small value, usually 1 to 3. Usually we just have to subtract 1 and we're good.

## Final Exploit Code
```python
from sympy import divisors
import gmpy2
import itertools

a = 0xe64a5f84e2762be5
chunk_size = 64

def gen_prime(bits, s):
    #s = random.getrandbits(chunk_size)
    while True:
        s |= 0xc000000000000001
        p = 0
        for _ in range(bits // chunk_size):
            p = (p << chunk_size) + s
            s = a * s % 2**chunk_size
        if gmpy2.is_prime(p):
            return p

n = 0xab802dca026b18251449baece42ba2162bf1f8f5dda60da5f8baef3e5dd49d155c1701a21c2bd5dfee142fd3a240f429878c8d4402f5c4c7f4bc630c74a4d263db3674669a18c9a7f5018c2f32cb4732acf448c95de86fcd6f312287cebff378125f12458932722ca2f1a891f319ec672da65ea03d0e74e7b601a04435598e2994423362ec605ef5968456970cb367f6b6e55f9d713d82f89aca0b633e7643ddb0ec263dc29f0946cfc28ccbf8e65c2da1b67b18a3fbc8cee3305a25841dfa31990f9aab219c85a2149e51dff2ab7e0989a50d988ca9ccdce34892eb27686fa985f96061620e6902e42bdd00d2768b14a9eb39b3feee51e80273d3d4255f6b19
c = 0x6a12d56e26e460f456102c83c68b5cf355b2e57d5b176b32658d07619ce8e542d927bbea12fb8f90d7a1922fe68077af0f3794bfd26e7d560031c7c9238198685ad9ef1ac1966da39936b33c7bb00bdb13bec27b23f87028e99fdea0fbee4df721fd487d491e9d3087e986a79106f9d6f5431522270200c5d545d19df446dee6baa3051be6332ad7e4e6f44260b1594ec8a588c0450bcc8f23abb0121bcabf7551fd0ec11cd61c55ea89ae5d9bcc91f46b39d84f808562a42bb87a8854373b234e71fe6688021672c271c22aad0887304f7dd2b5f77136271a571591c48f438e6f1c08ed65d0088da562e0d8ae2dadd1234e72a40141429f5746d2d41452d916
e = 65537
top = (n >> 1984) - 1 #12357927723151857701
bot = ((n % 2**64) * gmpy2.invert(13212876016237202665, 2**64)) % 2**64 #8695833300138004145

b = pow(a, 15, 2**64)

print top, bot
d = divisors((top << 64) + bot)

candidates = []
for num in d:
    if len(bin(num)) == 66:
        if num & 2**63 == 2**63 and num & 2**62 == 2**62 and num & 1 == 1:
            candidates.append(num)

for s1, s2 in itertools.combinations(candidates, 2):
    p = gen_prime(1024, s1)
    q = gen_prime(1024, s2)
    if p * q == n:
        d = gmpy2.invert(e, (p - 1) * (q - 1))
        print pow(c, d, n)
```
## Flag
```
CTF{__donald_knuths_lcg_would_be_better_well_i_dont_think_s0__}
```
