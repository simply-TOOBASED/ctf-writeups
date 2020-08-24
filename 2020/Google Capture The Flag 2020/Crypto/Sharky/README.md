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


