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
Now, assuming we have the output of the `compression_step` function, can we reverse it?
