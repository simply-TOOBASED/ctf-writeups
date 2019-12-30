# Challenge

We are given the following python script: [vuln.py](./vuln.py) and makefile [Makefile](Makefile), as well as a server to connect to: `nc 88.198.156.141 2833`

```python
#!/usr/bin/env python3
import os, ctypes

lib = ctypes.CDLL('./tweetnacl.so')

def mac(key, msg):
    tag = ctypes.create_string_buffer(16)
    lib.crypto_onetimeauth_poly1305_tweet(tag, key, len(msg), msg)
    return bytes(tag)

def chk(key, tag, msg):
    return not lib.crypto_onetimeauth_poly1305_tweet_verify(tag, key, len(msg), msg)


key = os.urandom(32)

for _ in range(32):
    msg = os.urandom(32)
    print(msg.hex(), mac(key, msg).hex())

msg = os.urandom(32)
print(msg.hex(), '?'*32)

tag = bytes.fromhex(input())

if not chk(key, tag, msg):
    print('no')
    exit(1)

print(open('flag.txt').read().strip())

```
```Makefile

tweetnacl.so:
	(: \
	&& curl -s https://tweetnacl.cr.yp.to/20140427/tweetnacl.h \
	&& curl -s https://tweetnacl.cr.yp.to/20140427/tweetnacl.c \
	&& echo '#include <sys/random.h>' \
	&& echo '#include <stdlib.h>' \
	&& echo 'void randombytes(u8 *p, u64 n)' \
	&& echo '{ for (ssize_t r = 0; n -= r; p += r)' \
	&& echo 'if (0 > (r = getrandom(p, n, 0))) exit(-1); }' \
	)| \
	fgrep -v tweetnacl.h | \
	gcc -shared -O2 -Wl,-s -fpic -xc - -o tweetnacl.so

clean:
	rm -f tweetnacl.so

```

## Initial Analysis
Looking a the python script, we know the following:
* We are dealing with a MAC-based scheme (https://en.wikipedia.org/wiki/Message_authentication_code)
* Key is randomly generated and is 32 bytes
* The server will give us 32 randomly generated messages as well their tags, all using the same key
* We are asked to find the correct tag for a given message and we get the flag

The point of the Makefile is to compile a shared library file `tweetnacl.so` so we can execute the C functions to generate and verify tags.

Looking at the source code for the `crypto_onetimeauth_poly1305_tweet` function (from https://tweetnacl.cr.yp.to/20140427/tweetnacl.c), we see the following (`crypto_onetimeauth_poly1305_tweet` is just an alias for `crypto_onetimeauth`):

```C
int crypto_onetimeauth(u8 *out,const u8 *m,u64 n,const u8 *k)
{
  u32 s,i,j,u,x[17],r[17],h[17],c[17],g[17];

  FOR(j,17) r[j]=h[j]=0;
  FOR(j,16) r[j]=k[j];
  r[3]&=15;
  r[4]&=252;
  r[7]&=15;
  r[8]&=252;
  r[11]&=15;
  r[12]&=252;
  r[15]&=15;

  while (n > 0) {
    FOR(j,17) c[j] = 0;
    for (j = 0;(j < 16) && (j < n);++j) c[j] = m[j];
    c[j] = 1;
    m += j; n -= j;
    add1305(h,c);
    FOR(i,17) {
      x[i] = 0;
      FOR(j,17) x[i] += h[j] * ((j <= i) ? r[i - j] : 320 * r[i + 17 - j]);
    }
    FOR(i,17) h[i] = x[i];
    u = 0;
    FOR(j,16) {
      u += h[j];
      h[j] = u & 255;
      u >>= 8;
    }
    u += h[16]; h[16] = u & 3;
    u = 5 * (u >> 2);
    FOR(j,16) {
      u += h[j];
      h[j] = u & 255;
      u >>= 8;
    }
    u += h[16]; h[16] = u;
  }

  FOR(j,17) g[j] = h[j];
  add1305(h,minusp);
  s = -(h[16] >> 7);
  FOR(j,17) h[j] ^= s & (g[j] ^ h[j]);

  FOR(j,16) c[j] = k[j + 16];
  c[16] = 0;
  add1305(h,c);
  FOR(j,16) out[j] = h[j];
  return 0;
}
```

Doing some more googling, we can find some more documentation on the function: https://libsodium.gitbook.io/doc/advanced/poly1305. According to that site,

> The crypto_onetimeauth() function authenticates a message `m` whose length is `n` using a secret key `k` (crypto_onetimeauth_KEYBYTES bytes) and puts the authenticator into `out` (crypto_onetimeauth_BYTES bytes).

## The Bug
The interesting thing to note here is that in [vuln.py](./vuln.py), the "message" and "key" are swapped! I.e. the key is used as the message, and the message is used as the key. This can be seen here:

```python
lib.crypto_onetimeauth_poly1305_tweet(tag, key, len(msg), msg)
```

The code should actually be:

```python
lib.crypto_onetimeauth_poly1305_tweet(tag, msg, len(msg), key)
```


This means that we have 32 tags for the same message but for 32 different keys, and we have to figure out what the message is so we can generate the correct tag for this message for another key to get our flag.

The authenticator used (https://en.wikipedia.org/wiki/Poly1305) seems to be secure, so we can't attack the cryptosystem directly. 

At first we tried using a z3/angr script with constraints to find a correct message, but our model was "unsat", so we weren't getting anywhere. The C code for generating the authenticator looked pretty ugly, so I instead tried finding a python version of the authenticator so we can better analyze what we are given.

[This](https://github.com/ph4r05/py-chacha20poly1305/blob/master/chacha20poly1305/poly1305.py) one looks very clean compared to the C code:

```python
# Copyright (c) 2015, Hubert Kario
#
# See the LICENSE file for legal information regarding use of this file.
"""Implementation of Poly1305 authenticator for RFC 7539"""

from .cryptomath import divceil

class Poly1305(object):

    """Poly1305 authenticator"""

    P = 0x3fffffffffffffffffffffffffffffffb # 2^130-5

    @staticmethod
    def le_bytes_to_num(data):
        """Convert a number from little endian byte format"""
        ret = 0
        for i in range(len(data) - 1, -1, -1):
            ret <<= 8
            ret += data[i]
        return ret

    @staticmethod
    def num_to_16_le_bytes(num):
        """Convert number to 16 bytes in little endian format"""
        ret = [0]*16
        for i, _ in enumerate(ret):
            ret[i] = num & 0xff
            num >>= 8
        return bytearray(ret)

    def __init__(self, key):
        """Set the authenticator key"""
        if len(key) != 32:
            raise ValueError("Key must be 256 bit long")
        self.acc = 0
        self.r = self.le_bytes_to_num(key[0:16])
        self.r &= 0x0ffffffc0ffffffc0ffffffc0fffffff
        self.s = self.le_bytes_to_num(key[16:32])

    def create_tag(self, data):
        """Calculate authentication tag for data"""
        for i in range(0, divceil(len(data), 16)):
            n = self.le_bytes_to_num(data[i*16:(i+1)*16] + b'\x01')
            self.acc += n
            self.acc = (self.r * self.acc) % self.P
        self.acc += self.s
        return self.num_to_16_le_bytes(self.acc)
```

So the key used is split up into 2 128-bit integers to form `r` and `s`, and then these are used with the message to form the authenticator. We know our message is 32 bytes, so `for i in range(0, divceil(len(data), 16))` turns into `for i in range(0, 2)`. Looking at the `create_tag` function, all it's doing is converting 16 bytes of our message to a number, and then adding it to the tag, which is then multiplied by `r` and taken modulo `P`, where P is `0x3fffffffffffffffffffffffffffffffb`. 

So we can define a `k1` and `k2` (2 128-bit integers which form the message), and then generate a modular equation in `k1` and `k2`, and then we can use a simple matrix to solve the modular equation.

## Caveat

However, although we can solve modular equations similar to solving regular equations (ie we only need 2 equations if we have 2 unknowns), the solution we get won't necessarily be right due to the fact that our solution is correct in modulo P, but since there are infinitely many values that are equal modulo P, our solution can be any one of them.

To show this concept, take the following modular equations

```
3x + y = 1 (mod 10)
9x + 5y = 7 (mod 10)
```

Accoring to [WolframAlpha](https://www.wolframalpha.com/input/?i=3x+%2B+y+%3D+1+%28mod+10%29%2C+9x+%2B+5y+%3D+7+%28mod+10%29), there are 2 solutions. This is just to show that just because we have 2 modular equations doesn't mean there's only 1 solution, sometimes there's none or more than 1.

This is why we're given 32 equations instead of just 2, because although there's a solution that satisfies all 32 equations, when we choose 2 of them to solve, we may get the "wrong" solution.

## Finding the "right" solution

The good news is that we can easily check if our solution correct or not. Our message is 32 bytes, which is split into 16 byte chunks (hence why the loop only runs twice), and "\x01" is added to each chunk before being converted to a number. What this means it that the MSB (most significant byte or 17th byte) should be 1 for both `k1` and `k2`. Once we figure out a candidate value for `k1` and `k2`, we make sure the 17th byte is 1, and then generate our tag.

We used Sage to do this, so we needed slghtly modify the `create_tag` function to handle symbolic variables. [Solution](./exploit.sage) code is below.

```sage
import binascii

class Poly1305(object):
    """Poly1305 authenticator"""

    P = 0x3fffffffffffffffffffffffffffffffb # 2^130-5

    @staticmethod
    def le_bytes_to_num(data):
        """Convert a number from little endian byte format"""
        ret = 0
        for i in range(len(data) - 1, -1, -1):
            ret <<= 8
            ret += ord(data[i])
        return ret

    @staticmethod
    def num_to_16_le_bytes(num):
        """Convert number to 16 bytes in little endian format"""
        ret = [0]*16
        for i, _ in enumerate(ret):
            ret[i] = num & 0xff
            num >>= 8
        return bytearray(ret)

    def __init__(self, key):
        """Set the authenticator key"""
        if len(key) != 32:
            raise ValueError("Key must be 256 bit long")
        self.acc = 0
        self.r = self.le_bytes_to_num(key[0:16])
        self.r &= 0x0ffffffc0ffffffc0ffffffc0fffffff
        self.s = self.le_bytes_to_num(key[16:32])

    def create_tag(self, data):
        """Calculate authentication tag for data"""
        for i in range(0, 2):
            #n = self.le_bytes_to_num(data[i*16:(i+1)*16] + b'\x01')
            n = data[i]
            self.acc += n
            self.acc = (self.r * self.acc)
        self.acc += self.s
        #print(hex(self.r), hex(self.s))
        #return self.num_to_16_le_bytes(self.acc)
        return self.acc

    def create_tag2(self, data):
        """Calculate authentication tag for data"""
        #print(range(0, 2)
        for i in range(0, 2):
            #print(data[i*16:(i+1)*16] + b'\x01')
            #n = self.le_bytes_to_num(data[i*16:(i+1)*16] + b'\x01')
            n = data[i]
            #print(hex(n))
            self.acc += n
            self.acc = (self.r * self.acc)
        self.acc += self.s
        #print(hex(self.r), hex(self.s))
        self.acc = self.acc % self.P
        return self.num_to_16_le_bytes(self.acc)

a = """693ab77a404b15bb22ed68fe9ea620149c4c9341a29ae5379cbc757f3d51e6c5 961b5f72be797f4611b8a4facaeea290
445cb5496654e15d615aefd80adf55ff60cf19e569ebfa88e908d78bc3bec9ec b975e601644a70efe16eab03dbcbbef0
8fb442bf439f41a4cf0e640f2142edc29bff57de32672a68567f31909a855567 5b96f1efa47a8b802338a7767af4ed87
acab5ab72da8f74e10c807adbd805a09cdac05fc7a73f8cbc2e41f01cf534fdb be2b48b1c0c4382bb603b39e80196e98
9058952f81bbdad286c687eed5faf817a20ff95d72ff84966d6d41a7b05078ee df48c0b1bfc43d0d16118967fd13d326
e0661c8b87802f273312f213a548b2def04062d0229fcffecd23170dc17c9af6 316dcab234b12214e096c4670be12e09
45be8a6aa4b638231ae353356498df5559f99730f1a2c2ea8b252f9623eaac38 b1d669c88c877b563bcc249e82974c72
97866592bae26c1506561b5f348f08d8564a4f426af9755aa1d3f1e107eb05e1 b344cf5b09f55d4cd2bd5ef0f7bed384
99f021bb3ff6e89a5d273feba7d63ecac67055a49eef9efe7b31375847e6ef76 82ae2b1406ce8d1b21513c97635e306c
69ddbf8f04dc4ad86bbbdab7b0ea66cbe2e7befa464c2acf00307d23072c0218 d8d6cb84bf7a0950077e3d2e11bbf0d1
5e14dde87c63089744cdcbb5df0b59e4a43eba0965569727a37ff0e5bc418568 c8bf4eda85e787db239c6b6528b70112
eb8f80a9c446128598c0eaafc7bae4486e132a34d17e962c8e8c00117998bea5 522dfa9851dbfbc32c5f65e2feb493b6
30e86f2bcad79c74f8a797a4e6c26cb5b6337ded92d8812761ceb134acfa57d0 8ca8793c685abb48b20f84d3486c51c7
0074ea65dd8ba85c66062bd6cc9fff4e26f4810a866699c7525bf708d3081f94 92f9afec6e392ff750e451da053c003e
b6c1c986d8b7962b08d7ca6027eb71ddbb9ec73422685442493cd77b4dce21df 496463c07dcc23f2f3722667e675f66a
b274820a15d76642d970012066c3b323ec9bdb5f439df3b73ebc6726631b44bd eb7c9f3c4f74e3450b77b3804ce76af6
ab0c7acf29d1c1b28021854d39e8d54e3987b45153ad4bd61a0f7686101cab0b f4656e3aee18c8a74f506e6a1c49c9a4
83186d8a4ba12f0b2bb9a58bd62c6ab05db186f006de7e52f0ce592daa207c30 b0dac69177920eea87fd1ea8145775f0
6cc5368a229523891979b6b21a224e31ce33e5d22a518949a477aa6bb2ff4713 5e29e291586640ba897fff491450352a
ba65443d09082edce8ec015dfb75f6c3f7716cf3018331e8ee16ca8446338dcc bf2a9e6f4efada9aece11cc0736c0623
c0dfc098813b9224b190c53f1f83537f7091f9a4c0046e417174f09b583e7781 3ae181536cfe50da24316806ef5f22bc
5eebc27424397bc0cbd12b17c0bb13ef16bf41ee0f6e56a34f984ac23d11982f 8ce6efc30668ace45eaddab5a32ced53
1e96cd0ff343976d814612294cfaab4c05c7fff4a7e12d5031081d76534c01b2 014f6e0af078c07def60c6837f605fa3
36004a6487b911c624a3ad87c0060a6ef3539c0fe6caf85d8b6754a4f294d06c d513ea25a734ab8f8f63ebd529f590d2
467349e704b4d69428495a842fb8071444ef060f7f697b22ecae62ecc0c46eac 4902b4071e6e2855c4e73409d4d03dac
3b844aa0580cea205b89118511fa04435ae2c630fd4d74088c10d8337ded2f8a d82d99a137b95bd5fbe12508729db4d3
5346a335709a52a4656e40e556b2528009d3c28564fd3a1244c50de436e6511f 2946ecf3539917bdcdcba9970e0ddd76
08c5344274eb5c53a3ab8dc0916976e15ad833a552db37dd5cb94d53b15c7ee6 f175911dad2d04c6aee9585e3be71675
448a5ba5f9db5b0e5ea576b9c6c8835ddde3076dae429f90963cc08fbd40a947 90411a2cbbbad82939b60b6dd9fd824a
6ef2a9298bceee61af7c538348c2533a8bd60b304de93c48c91c0237af9457cf 5d0774bbde55116c0231b754c202dc55
1876e6ebb1760cbf09a667e1f18f5d205e1fd0da86668bac866af9f2522efd6b 126ef6463bbd0add73edde3559af14c1
0160ee3b996e75f4853bb91374687d8c91975f72e7734f5e6e447c95d6777610 61f8ad0ec017aee48f7ae5107c22f32c"""

correct = [x.split(" ") for x in a.split("\n")]
for inc in xrange(16):
    eqns =[]
    eqns2 =[]
    for _ in xrange(2*inc, 2*inc + 2):
        authy  = Poly1305(correct[_][0].decode('hex'))
        key = list(var('k_%d' % i) for i in range(2))
        #R = PolynomialRing(GF(0x3fffffffffffffffffffffffffffffffb), names='k_0, k_1')
        
        _tag = authy.create_tag(key)
        constant_term = _tag.coefficient(k_0, 0).coefficient(k_1, 0)
        eqns2.append([_tag.coefficient(k_0, 1), _tag.coefficient(k_1, 1)])
        eqns.append(authy.le_bytes_to_num((correct[_][1] + "01").decode('hex')) - constant_term)

    A = matrix(GF(0x3fffffffffffffffffffffffffffffffb), eqns2)
    b = matrix(GF(0x3fffffffffffffffffffffffffffffffb), eqns).transpose()
    #print A
    #print b
    k1 = long(list(A.solve_right(b))[0][0])
    k2 = long(list(A.solve_right(b))[1][0])

    if k1 >> 128 == 1 and k2 >> 128 == 1:
        print k1, k2
        authy  = Poly1305("e8282a30b28c2cd29d8d8b38bd2a6fbc3082c316c17c867c7f6377caa883af3c".decode('hex'))
        print binascii.hexlify(authy.create_tag2([k1, k2]))
        break

```

Because there's no time limit to input our answer, we just copy-and-pasted the values we needed from the server and then manually inputted the correct tag.
