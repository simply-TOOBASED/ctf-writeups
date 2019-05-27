**DISCLAIMER**: Solved after competition ended.

# Challenge

We are given the following python script: [cactus.py](./cactus.py).

```
import random


class Oracle:

    def __init__(self, secret, bits=512):
        self.secret = secret #10
        self.bits = bits #512
        self.range = 2*self.bits #1024

    def sample(self, w):
        r = random.randint(0, 2^self.range) #random number from 0 to 1026 inclusive
        idx = range(self.bits) #range(512)
        random.shuffle(idx) #shuffle range(512)
        e = sum(1<<i for i in idx[:w]) #in binary, e should have 10 "1"s 
        return self.secret*r+e #10*r + e


o = Oracle(10)
for i in range(100):
    print o.sample(10)
```

Couple of things to note:
1. There's a bug with the line `random.randint(0, 2^self.range)`. It was probably intended to use exponentiation but in python the ^ operator means xor, so what's actually caluclated is 2 xor 1024 not 2 to the power of 1024.
2.
