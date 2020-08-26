# Challenge

Can you find the round keys?
```
sharky.2020.ctfcompetition.com 1337
```
We are given 2 files, [sha256.py](sha256.py) and [challenge.py](challenge.py). [sha256.py](sha256.py) is a custom implementation of the sha256 hashing algorithm, specifically what's different is the fact that we're allowed to specify custom round keys, instead of the default ones.

By default these are the rounds keys that are used:
```python
self.k = [
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1,
        0x923f82a4, 0xab1c5ed5, 0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786,
        0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147,
        0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85, 0xa2bfe8a1, 0xa81a664b,
        0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a,
        0x5b9cca4f, 0x682e6ff3, 0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    ]
```
This function is used to change the default round keys:
```python
def sha256_raw(self, m, round_keys = None):
    if len(m) % 64 != 0:
      raise ValueError('m must be a multiple of 64 bytes')
    state = self.h
    for i in range(0, len(m), 64):
      block = m[i:i + 64]
      w = self.compute_w(block)
      s = self.compression(state, w, round_keys)
      state = [(x + y) & 0xffffffff for x, y in zip(state, s)]
    return state
```
Other than custom round keys, the implementation is standard sha256. 

Looking at [challenge.py](challenge.py), we see the code generates 8 random round keys, and sets them as the first 8 values of the `self.k` array. Then this modified sha256 is used with encrypting a message, and we have to correctly guess the 8 random rounds keys to get the flag.

```python
#! /usr/bin/python3
import binascii
import os
import sha256

# Setup msg_secret and flag
FLAG_PATH = 'data/flag.txt'
NUM_KEYS = 8
MSG = b'Encoded with random keys'

with open(FLAG_PATH, 'rb') as f:
  FLAG = f.read().strip().decode('utf-8')


def sha256_with_secret_round_keys(m: bytes, secret_round_keys: dict) -> bytes:
  """Computes SHA256 with some secret round keys.

  Args:
    m: the message to hash
    secret_round_keys: a dictionary where secret_round_keys[i] is the value of
      the round key k[i] used in SHA-256

  Returns:
    the digest
  """
  sha = sha256.SHA256()
  round_keys = sha.k[:]
  for i, v in secret_round_keys.items():
    round_keys[i] = v
  return sha.sha256(m, round_keys)


def generate_random_round_keys(cnt: int):
  res = {}
  for i in range(cnt):
    rk = 0
    for b in os.urandom(4):
      rk = rk * 256 + b
    res[i] = rk
  return res

if __name__ == '__main__':
  secret_round_keys = generate_random_round_keys(NUM_KEYS)
  digest = sha256_with_secret_round_keys(MSG, secret_round_keys)
  print('MSG Digest: {}'.format(binascii.hexlify(digest).decode()))
  GIVEN_KEYS = list(map(lambda s: int(s, 16), input('Enter keys: ').split(',')))
  assert len(GIVEN_KEYS) == NUM_KEYS, 'Wrong number of keys provided.'

  if all([GIVEN_KEYS[i] == secret_round_keys[i] for i in range(NUM_KEYS)]):
    print('\nGood job, here\'s a flag: {0}'.format(FLAG))
  else:
    print('\nSorry, that\'s not right.')
```

Couple of things to note:
* The message that's encrypted is the same everytime `MSG = b'Encoded with random keys'`. This was verified with an admin.
* There are 8 round keys we need to guess, each of them is a 32-bit (4-byte) random value.
* Because the message is the same everytime, we can compute some of the values in the sha256 hashing algorithm (as they are constant) and reverse each round, getting us the flag.

## sha256 Hashing Algorithm

We will go into the hashing algorithm for sha256 and prove how encrypting the same message allows us to reverse the rounds.
```python
def sha256_raw(self, m, round_keys = None):
    if len(m) % 64 != 0:
      raise ValueError('m must be a multiple of 64 bytes')
    state = self.h
    for i in range(0, len(m), 64):
      block = m[i:i + 64]
      w = self.compute_w(block)
      s = self.compression(state, w, round_keys)
      state = [(x + y) & 0xffffffff for x, y in zip(state, s)]
    return state
```

The first thing that happens is our input is checked for length and if it's not 64 bytes it's padded to achieve this length. Because our input is always `b'Encoded with random keys'`, it's obvious it's going to be padded. After padding our message looks like
```
b'Encoded with random keys\x80\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\xc0'
```
This also means that our message is always one block, so we only have to worry about the loop running once. The next thing that happens is that the `w` array is computed through the function `w = self.compute_w(block)`.
```python
  def compute_w(self, m):
    w = list(struct.unpack('>16L', m))
    for _ in range(16, 64):
      a, b = w[-15], w[-2]
      s0 = self.rotate_right(a, 7) ^ self.rotate_right(a, 18) ^ (a >> 3)
      s1 = self.rotate_right(b, 17) ^ self.rotate_right(b, 19) ^ (b >> 10)
      s = (w[-16] + w[-7] + s0 + s1) & 0xffffffff
      w.append(s)
    return w
```
Looking at the function, the only thing it's dependent on is `m`, or the 64-byte block of the message. Because our message is the same, the `w` array is also the same and we can compute this ourselves. You should get this `w` array when you compute it with the padded-message above:
```
w = [1164862319, 1684366368, 2003399784, 544366958, 1685024032, 1801812339, 2147483648, 0, 0, 0, 0, 0, 0, 0, 0, 192, 1522197188, 3891742175, 3836386829, 32341671, 928288908, 2364323079, 1515866404, 649785226, 1435989715, 250124094, 1469326411, 2429553944, 598071608, 1634056085, 4271828083, 4262132921, 2272436470, 39791740, 2337714294, 3555435891, 1519859327, 57013755, 2177157937, 1679613557, 2900649386, 612096658, 172526146, 2214036567, 3330460486, 1490972443, 1925782519, 4215628757, 2379791427, 2058888203, 1834962275, 3917548225, 2375084030, 1546202149, 3188006334, 4280719833, 726047027, 3650106516, 4058756591, 1443098026, 1972312730, 1218108430, 3428722156, 366022263]
```
Again, this stays constant since the message is constant. After the `w` array is computed, we run the compression function which is just a loop that runs the compression step function 64 times
```python
  def compression_step(self, state, k_i, w_i):
    a, b, c, d, e, f, g, h = state
    s1 = self.rotate_right(e, 6) ^ self.rotate_right(e, 11) ^ self.rotate_right(e, 25)
    ch = (e & f) ^ (~e & g)
    tmp1 = (h + s1 + ch + k_i + w_i) & 0xffffffff
    s0 = self.rotate_right(a, 2) ^ self.rotate_right(a, 13) ^ self.rotate_right(a, 22)
    maj = (a & b) ^ (a & c) ^ (b & c)
    tmp2 = (tmp1 + s0 + maj) & 0xffffffff
    tmp3 = (d + tmp1) & 0xffffffff
    return (tmp2, a, b, c, tmp3, e, f, g)

  def compression(self, state, w, round_keys = None):
    if round_keys is None:
      round_keys = self.k
    for i in range(64):
      state = self.compression_step(state, round_keys[i], w[i])
    return state
```
Let's look closely at the `compression_step` function.
* There is a state of 8 inputs.
* The initial value of `state` is based off of the `self.h` array (which is constant and provided)
* `tmp1`, `tmp2`, and `tmp3` are the values directly affected by `k_i`, which is the round key (and we don't know the first 8)

## Reversing compression_step
Now, assuming we have the output of the `compression_step` function, can we reverse it? This means we have
```
tmp2, a, b, c, tmp3, e, f, g
```
and we want to recover
```
a, b, c, d, e, f, g, h
```
So we can already compute 4 values in the `compression_step`, since they are directly computed from the values we have.
```
s0 = self.rotate_right(a, 2) ^ self.rotate_right(a, 13) ^ self.rotate_right(a, 22)
ch = (e & f) ^ (~e & g)
s1 = self.rotate_right(e, 6) ^ self.rotate_right(e, 11) ^ self.rotate_right(e, 25)
maj = (a & b) ^ (a & c) ^ (b & c)
```
Even though the `& 0xffffffff` computation looks like it can't be reversed, it's actually just equivalent to `% 2**32`, or mod 4294967296. 

Because all of the values in `state` and that are computed in `compression_step` are at most 32-bits (meaning they are smaller than 4294967296), we can reverse the `& 0xffffffff` computation by simply doing a negative mod. In python3 this is trivial because negative numbers with the mod operator result in a positive number (ie -3 mod 5 = 2). 

Also, because we have `tmp2` and `tmp3`, we can recover `tmp1` and then `h` and `d`. Remember that `w_i` references one of the values from our `w` array we talked about earlier. Because this is constant, we can recover `h`.
```
tmp1 = (tmp2 - s0 - maj) % 2**32
h = (tmp1 - s1 - ch - k_i - w_i) % 2**32
d = (tmp3 - tmp1) % 2**32  
```
So we were able to successfully recover all of the values, simply due to the fact that the message encrypted is always the same and then the `w` array is also the same. Function to reverse a compression step is below (we add this function to [sha256.py](sha256.py)):
```python
  def compression_step_inv(self, state, k_i, w_i):
    tmp2, a, b, c, tmp3, e, f, g = state
    s0 = self.rotate_right(a, 2) ^ self.rotate_right(a, 13) ^ self.rotate_right(a, 22)
    ch = (e & f) ^ (~e & g)
    s1 = self.rotate_right(e, 6) ^ self.rotate_right(e, 11) ^ self.rotate_right(e, 25)
    maj = (a & b) ^ (a & c) ^ (b & c)
    tmp1 = (tmp2 - s0 - maj) % 2**32
    h = (tmp1 - s1 - ch - k_i - w_i) % 2**32
    d = (tmp3 - tmp1) % 2**32  
    #print("Compression step:", list(map(hex, (a, b, c, d, e, f, g, h))))
    return (a, b, c, d, e, f, g, h)
```
So we can reverse a compression step, but there's a problem. We don't know the `k_i` values for the first 8 rounds (that's what we need to recover), so when we go backwards and hit the 8th round we're gonna get stuck. We turn to our trusty solver, z3!

We can essentially create 8 BitVectors that are 32-bits (to represent the unknown 8 round keys), and then feed them into our `compression_step_inv` function, creating 8 equations. We can build a model with these equations, since we know what the final output_state needs to look like, and then see if z3 can figure out our round keys. Remember that the final output state (going backwards) needs to be `self.h`
```
self.h = [
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c,
        0x1f83d9ab, 0x5be0cd19
    ]
```
Beacuse this is the initial value of `state` used for the first time `compression_step` is executed. So we have a target value that our model needs to satisfy. Before we build the z3 model, we need to first figure out some other small things.

First, we need to figure out the last `state` value after `compression_step` is executed 64 times. If we go back to the sha256-algorithm, there's one more thing that happens before we're given our hash.

```python
  def sha256_raw(self, m, round_keys = None):
    if len(m) % 64 != 0:
      raise ValueError('m must be a multiple of 64 bytes')
    print(m)
    state = self.h
    for i in range(0, len(m), 64):
      block = m[i:i + 64]
      w = self.compute_w(block)
      s = self.compression(state, w, round_keys)
      state = [(x + y) & 0xffffffff for x, y in zip(state, s)]
    return state
```
The last output state is `s`, which we need to find out, and our hash is computed by adding each value in `s` to each value in `state`, which is `self.h`. So to recover `s`, we take our hash, unpack it in an array of 8 32-bit integers, and then do the same negative mod we did earlier. We can do it with the following code.
```python
final_state = [0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19]
hh = "39715f0da097fc779d86e4ec5221d19cec1d908d219e725b929ff540158da0c0"
unpacked_digest = []
for i in range(0, len(hh), 8):
    unpacked_digest.append(int(hh[i:i + 8], 16))
s = [(x - y) % 2**32 for x, y in zip(unpacked_digest, final_state)]
```
Now that we have the correct `s` value, we can run our `compression_step_inv` function and build our z3 model.
## Building the z3 model and recovering the round keys
```python
sha = sha256.SHA256()
final_state = [0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19]
hh = "39715f0da097fc779d86e4ec5221d19cec1d908d219e725b929ff540158da0c0"
unpacked_digest = []
for i in range(0, len(hh), 8):
    unpacked_digest.append(int(hh[i:i + 8], 16))
last_state = [(x - y) % 2**32 for x, y in zip(unpacked_digest, final_state)]
for _ in range(-1, -57, -1):
    last_state = sha.compression_step_inv(last_state, round_k[_], w[_])

s = Solver()  
#now we use z3 to figure out the last 8
k_s = [BitVec("k{}".format(i), 32) for i in range(8)]
for _ in range(7, -1, -1):
    last_state = compression_step_inv_z3(last_state, k_s[_], w[_])
for _ in range(8):
    s.add(final_state[_] == last_state[_])

s.check()
m = s.model()
recovered_round_keys = [m[ki].as_long() for ki in k_s]
print(recovered_round_keys)
```
Now we get to put together what we described. We first figure out the last output of the `compress_step` function, then we run our `compression_step_inv` function 56 times. For the last 8 times, we use our 8 BitVectors to hold the unknown round keys. Then we run the `compression_step_inv_z3` function 8 times to build our model. The reason we have a slightly different inverse function is because some computations don't port well with z3, so we have to make sure we use basic operators like bit shifts, &, |, etc. After building our model, we ask z3 to find some values that satisfy it.

```python
def rotate_right(v, n):
  w = (v >> n) | (v << (32 - n))
  return w & 0xffffffff

def compression_step_inv_z3(state, k_i, w_i):
  tmp2, a, b, c, tmp3, e, f, g = state
  s0 = rotate_right(a, 2) ^ rotate_right(a, 13) ^ rotate_right(a, 22)
  ch = (e & f) ^ (~e & g)
  s1 = rotate_right(e, 6) ^ rotate_right(e, 11) ^ rotate_right(e, 25)
  maj = (a & b) ^ (a & c) ^ (b & c)
  tmp1 = (tmp2 - s0 - maj) % 2**32
  h = (tmp1 - s1 - ch - k_i - w_i) % 2**32
  d = (tmp3 - tmp1) % 2**32  
  #print("Compression step:", list(map(hex, (a, b, c, d, e, f, g, h))))
  return (a, b, c, d, e, f, g, h)
```
Running the code, we successfully do get potential round_key values, but they don't seem to be correct. Manually testing, we find out that 1 of the values (usually the 2nd round key) is incorrect. 

The reason for this is simply because our model wasn't constrained enough, so z3 found other potential solutions. We have 2 choices, either add more constraints (which i will discuss), or just kepe querying the server until z3 correctly guesses the round keys (which i ended up doing).

## Final Exploit Code
```python
#! /usr/bin/python3
import binascii
import os
import sha256
import hashlib
from z3 import *
from pwn import *

# Setup msg_secret and flag
NUM_KEYS = 8
MSG = b'Encoded with random keys'

def sha256_with_secret_round_keys(m: bytes, secret_round_keys: dict) -> bytes:
  """Computes SHA256 with some secret round keys.

  Args:
    m: the message to hash
    secret_round_keys: a dictionary where secret_round_keys[i] is the value of
      the round key k[i] used in SHA-256

  Returns:
    the digest
  """
  sha = sha256.SHA256()
  round_keys = sha.k[:]
  for i, v in secret_round_keys.items():
    round_keys[i] = v
  return sha.sha256(m, round_keys)


def generate_random_round_keys(cnt: int):
  res = {}
  for i in range(cnt):
    rk = 0
    for b in os.urandom(4):
      rk = rk * 256 + b
    res[i] = rk
  return res

round_k = [
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1,
        0x923f82a4, 0xab1c5ed5, 0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786,
        0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147,
        0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85, 0xa2bfe8a1, 0xa81a664b,
        0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a,
        0x5b9cca4f, 0x682e6ff3, 0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    ]

w = [1164862319, 1684366368, 2003399784, 544366958, 1685024032, 1801812339, 2147483648, 0, 0, 0, 0, 0, 0, 0, 0, 192, 1522197188, 3891742175, 3836386829, 32341671, 928288908, 2364323079, 1515866404, 649785226, 1435989715, 250124094, 1469326411, 2429553944, 598071608, 1634056085, 4271828083, 4262132921, 2272436470, 39791740, 2337714294, 3555435891, 1519859327, 57013755, 2177157937, 1679613557, 2900649386, 612096658, 172526146, 2214036567, 3330460486, 1490972443, 1925782519, 4215628757, 2379791427, 2058888203, 1834962275, 3917548225, 2375084030, 1546202149, 3188006334, 4280719833, 726047027, 3650106516, 4058756591, 1443098026, 1972312730, 1218108430, 3428722156, 366022263]

def rotate_right(v, n):
  w = (v >> n) | (v << (32 - n))
  return w & 0xffffffff

def compression_step_inv_z3(state, k_i, w_i):
  tmp2, a, b, c, tmp3, e, f, g = state
  s0 = rotate_right(a, 2) ^ rotate_right(a, 13) ^ rotate_right(a, 22)
  ch = (e & f) ^ (~e & g)
  s1 = rotate_right(e, 6) ^ rotate_right(e, 11) ^ rotate_right(e, 25)
  maj = (a & b) ^ (a & c) ^ (b & c)
  tmp1 = (tmp2 - s0 - maj) % 2**32
  h = (tmp1 - s1 - ch - k_i - w_i) % 2**32
  d = (tmp3 - tmp1) % 2**32  
  #print("Compression step:", list(map(hex, (a, b, c, d, e, f, g, h))))
  return (a, b, c, d, e, f, g, h)

if __name__ == '__main__':
  while True:
    sha = sha256.SHA256()
    r = remote('sharky.2020.ctfcompetition.com', 1337)
    r.recvuntil("MSG Digest: ")
    final_state = [0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19]
    hh = r.recvline().strip()
    unpacked_digest = []
    for i in range(0, len(hh), 8):
      unpacked_digest.append(int(hh[i:i + 8], 16))
    last_state = [(x - y) % 2**32 for x, y in zip(unpacked_digest, final_state)]
    for _ in range(-1, -57, -1):
      last_state = sha.compression_step_inv(last_state, round_k[_], w[_])
    
    s = Solver()  
    #now we use z3 to figure out the last 8 round_keys
    k_s = [BitVec("k{}".format(i), 32) for i in range(8)]
    for _ in range(7, -1, -1):
      last_state = compression_step_inv_z3(last_state, k_s[_], w[_])
    for _ in range(8):
      s.add(final_state[_] == last_state[_])
      
    s.check()
    m = s.model()
    state = [m[ki].as_long() for ki in k_s]
    r.recvuntil("Enter keys: ")
    r.sendline(', '.join(list(map(hex, state))))
    r.recvline()
    resp = r.recvline()
    if b"Sorry" not in resp:
        print(resp)
        break
    r.close()
```
```python
#! /usr/bin/python3
import struct

class SHA256:

  def __init__(self):
    self.h = [
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c,
        0x1f83d9ab, 0x5be0cd19
    ]

    self.k = [
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1,
        0x923f82a4, 0xab1c5ed5, 0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786,
        0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147,
        0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85, 0xa2bfe8a1, 0xa81a664b,
        0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a,
        0x5b9cca4f, 0x682e6ff3, 0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    ]

  def rotate_right(self, v, n):
    w = (v >> n) | (v << (32 - n))
    return w & 0xffffffff

  def compression_step(self, state, k_i, w_i):
    a, b, c, d, e, f, g, h = state
    s1 = self.rotate_right(e, 6) ^ self.rotate_right(e, 11) ^ self.rotate_right(e, 25)
    ch = (e & f) ^ (~e & g)
    tmp1 = (h + s1 + ch + k_i + w_i) & 0xffffffff
    s0 = self.rotate_right(a, 2) ^ self.rotate_right(a, 13) ^ self.rotate_right(a, 22)
    maj = (a & b) ^ (a & c) ^ (b & c)
    tmp2 = (tmp1 + s0 + maj) & 0xffffffff
    tmp3 = (d + tmp1) & 0xffffffff
    #print("Compression step:", list(map(hex, (tmp2, a, b, c, tmp3, e, f, g))))
    return (tmp2, a, b, c, tmp3, e, f, g)
    
  def compression_step_inv(self, state, k_i, w_i):
    tmp2, a, b, c, tmp3, e, f, g = state
    s0 = self.rotate_right(a, 2) ^ self.rotate_right(a, 13) ^ self.rotate_right(a, 22)
    ch = (e & f) ^ (~e & g)
    s1 = self.rotate_right(e, 6) ^ self.rotate_right(e, 11) ^ self.rotate_right(e, 25)
    maj = (a & b) ^ (a & c) ^ (b & c)
    tmp1 = (tmp2 - s0 - maj) % 2**32
    h = (tmp1 - s1 - ch - k_i - w_i) % 2**32
    d = (tmp3 - tmp1) % 2**32  
    #print("Compression step:", list(map(hex, (a, b, c, d, e, f, g, h))))
    return (a, b, c, d, e, f, g, h)

  def compression(self, state, w, round_keys = None):
    if round_keys is None:
      round_keys = self.k
    for i in range(64):
      state = self.compression_step(state, round_keys[i], w[i])
    return state

  def compute_w(self, m):
    w = list(struct.unpack('>16L', m))
    for _ in range(16, 64):
      a, b = w[-15], w[-2]
      s0 = self.rotate_right(a, 7) ^ self.rotate_right(a, 18) ^ (a >> 3)
      s1 = self.rotate_right(b, 17) ^ self.rotate_right(b, 19) ^ (b >> 10)
      s = (w[-16] + w[-7] + s0 + s1) & 0xffffffff
      w.append(s)
    return w

  def padding(self, m):
    lm = len(m)
    lpad = struct.pack('>Q', 8 * lm)
    lenz = -(lm + 9) % 64
    return m + bytes([0x80]) + bytes(lenz) + lpad

  def sha256_raw(self, m, round_keys = None):
    if len(m) % 64 != 0:
      raise ValueError('m must be a multiple of 64 bytes')
    print(m)
    state = self.h
    for i in range(0, len(m), 64):
      block = m[i:i + 64]
      w = self.compute_w(block)
      s = self.compression(state, w, round_keys)
      state = [(x + y) & 0xffffffff for x, y in zip(state, s)]
    return state

  def sha256(self, m, round_keys = None):
    m_padded = self.padding(m)
    state = self.sha256_raw(m_padded, round_keys)
    #print(state)
    return struct.pack('>8L', *state)
```

## Flag 
`CTF{sHa_roUnD_k3Ys_caN_b3_r3vERseD}`

## BONUS: More constraints
For discussion purposes, I will explain how you can add more constraints to the z3 model. If we manually try to encode the same string with random round keys, we notice a pattern in the first 8 rounds, and a general pattern that applies to every round:
### Test Run 1
```
Compression step: ['0x8d09f2a5', '0x6a09e667', '0xbb67ae85', '0x3c6ef372', '0x29c94cfa', '0x510e527f', '0x9b05688c', '0x1f83d9ab']
Compression step: ['0x30e24489', '0x8d09f2a5', '0x6a09e667', '0xbb67ae85', '0xf0a2ce84', '0x29c94cfa', '0x510e527f', '0x9b05688c']
Compression step: ['0x9f70f7b', '0x30e24489', '0x8d09f2a5', '0x6a09e667', '0xbbf1a468', '0xf0a2ce84', '0x29c94cfa', '0x510e527f']
Compression step: ['0xb01abed9', '0x9f70f7b', '0x30e24489', '0x8d09f2a5', '0xaaa8fe56', '0xbbf1a468', '0xf0a2ce84', '0x29c94cfa']
Compression step: ['0x9050b03a', '0xb01abed9', '0x9f70f7b', '0x30e24489', '0xfc384a63', '0xaaa8fe56', '0xbbf1a468', '0xf0a2ce84']
Compression step: ['0x88be8cc6', '0x9050b03a', '0xb01abed9', '0x9f70f7b', '0xc24dce0a', '0xfc384a63', '0xaaa8fe56', '0xbbf1a468']
Compression step: ['0xde572332', '0x88be8cc6', '0x9050b03a', '0xb01abed9', '0x1a0a78ec', '0xc24dce0a', '0xfc384a63', '0xaaa8fe56']
Compression step: ['0xe3f7aecc', '0xde572332', '0x88be8cc6', '0x9050b03a', '0x92bdc67', '0x1a0a78ec', '0xc24dce0a', '0xfc384a63']
```
### Test Run 2
```
Compression step: ['0x515734e9', '0x6a09e667', '0xbb67ae85', '0x3c6ef372', '0xee168f3e', '0x510e527f', '0x9b05688c', '0x1f83d9ab']
Compression step: ['0xec1cf5af', '0x515734e9', '0x6a09e667', '0xbb67ae85', '0xfd775f76', '0xee168f3e', '0x510e527f', '0x9b05688c']
Compression step: ['0x4a9eebae', '0xec1cf5af', '0x515734e9', '0x6a09e667', '0x7839c308', '0xfd775f76', '0xee168f3e', '0x510e527f']
Compression step: ['0x9e95822c', '0x4a9eebae', '0xec1cf5af', '0x515734e9', '0xc051bae', '0x7839c308', '0xfd775f76', '0xee168f3e']
Compression step: ['0xab7e4af5', '0x9e95822c', '0x4a9eebae', '0xec1cf5af', '0xcd6f75d3', '0xc051bae', '0x7839c308', '0xfd775f76']
Compression step: ['0x896cb0e3', '0xab7e4af5', '0x9e95822c', '0x4a9eebae', '0x2691bc04', '0xcd6f75d3', '0xc051bae', '0x7839c308']
Compression step: ['0x44def41f', '0x896cb0e3', '0xab7e4af5', '0x9e95822c', '0x2c7c7370', '0x2691bc04', '0xcd6f75d3', '0xc051bae']
Compression step: ['0xd716cf20', '0x44def41f', '0x896cb0e3', '0xab7e4af5', '0xe20f7972', '0x2c7c7370', '0x2691bc04', '0xcd6f75d3']
```
In the first iteration of `compression_step`, there are 6 of the 8 values that stay the same, because the other 2 values are dependent on `k_i` which is the only thing that changes. In addition, a value from one iteration will show up in the spot to the right in the next iteration for certain indices. Combining these 2 facts, tells us this:
```
Compression step: ['x', '0x6a09e667', '0xbb67ae85', '0x3c6ef372', 'x', '0x510e527f', '0x9b05688c', '0x1f83d9ab']
Compression step: ['x', 'x', '0x6a09e667', '0xbb67ae85', 'x', 'x', '0x510e527f', '0x9b05688c']
Compression step: ['x', 'x', 'x', '0x6a09e667', 'x', 'x', 'x', '0x510e527f']
Compression step: ['x', 'x', 'x', 'x', 'x', 'x', 'x', 'x']
Compression step: ['x', 'x', 'x', 'x', 'x', 'x', 'x', 'x']
Compression step: ['x', 'x', 'x', 'x', 'x', 'x', 'x', 'x']
Compression step: ['x', 'x', 'x', 'x', 'x', 'x', 'x', 'x']
Compression step: ['x', 'x', 'x', 'x', 'x', 'x', 'x', 'x']
```
Because these values stay the same for the first 3 rounds for every encryption, we can add these constraints to our model and have a more reliable script to find the round keys.
