>Can you read my mind?<br><br>Challenge running at https://mindreader.web.ctfcompetition.com/

At first look at https://mindreader.web.ctfcompetition.com/, there's nothing really there, just one input and a question asking us what we want to read.

![Imgur](http://i.imgur.com/ftlJy65.png)

This is the source code for the website:

```html
<html>
<head>
</head>
<body>
    <p>Hello, what do you want to read?</p>
    <form method="GET">
        <input type="text" name="f">
        <input type="submit" value="Read">
    </form>
</body>
</html>
```

Just putting in random strings seems to be returning a 404 saying "URL not found". Example: https://mindreader.web.ctfcompetition.com/?f=flag

![Imgur](http://i.imgur.com/fZf9BWR.png)

So what happens if we input an actual URL? Trying https://mindreader.web.ctfcompetition.com/?f=http://www.google.com just returns the same 404. However, what happens if we try a file on the website? (hence the "what do you want to read?" part - it's asking what file we want to read) Trying obvious file names (index.php, index.html, etc.), and we see that https://mindreader.web.ctfcompetition.com/?f=index.html with "index.html" as an input string shows us the site we first saw, so that confirms that the website is a file reader. 

This kind of vulnerability is normally seen in other CTFs, and it's called LFI (Local File Includes). It basically means that the attacker (us) can see files on the web server that normally cannot be seen (such as `/etc/passwd/`), and this normally happens because user input isn't sanitized, and if the source code of the application opens files based on the file names inputted by us, and it's not sanitizing our input, then we can view any file we want on the webserver, which is typically something you do not want as then you can get access to passwords, private data, etc.

Next step is to try and guess what kind of web server or framework the website is running, i.e. something like Flask, Apache, NodeJS, etc., so we have to guess more filenames that could be on the webserver. https://mindreader.web.ctfcompetition.com/?f=requirements.txt seems to work, and tells us that the website is running off of Flask, a popular python web framework. 

![Imgur](http://i.imgur.com/Vof1bja.png)

Flask runs off of an app.py or a main.py, normally, so we try both of those filenames, and we find https://mindreader.web.ctfcompetition.com/?f=main.py! The reason the code isn't formatted is because it's just printing the code as plaintext, so I re-formatted the code and put in [main.py](main.py).

```python
from flask import Flask, request, abort
import re
import os
import logging

assert os.environ['FLAG']

app = Flask(__name__)

INDEX = open('index.html').read()

HALL_OF_SHAME = [
    '173.171.203.59',
    '2a02:6b8:b010:6026:84:201:185:197',
    '35.185.158.159',
    '81.17.25.8'
]

@app.route('/')
def index():
    for ip in request.headers.get('X-Forwarded-For', '').split(','):
        ip = ip.strip().lower()
        if ip in HALL_OF_SHAME:
            abort(403)

    if 'f' in request.args:
        try:
            f = request.args['f']
            if re.search(r'proc|random|zero|stdout|stderr', f):
                abort(403)
            elif '\x00' in f:
                abort(404)
            return open(f).read(4096)
        except IOError:
            abort(404)
    else:
        return INDEX
```

`assert os.environ['FLAG']` tells us that the flag we seek is stored in an environment variable. After some googling we see that we have to access `/proc/self/environ` in order to view any environment variables. However, lines 29 & 30 in our [main.py](main.py) seems to be protecting against this:

```python
if re.search(r'proc|random|zero|stdout|stderr', f):
    abort(403)
```

After being stuck for a couple of hours, my teammate gave me some useful information: `/dev/fd` is a symlink to `/proc/self/fd`. Therefore we can just make our filename `/dev/fd/../environ`, which is equivalent to `/proc/self/environ` and get the flag!

As predicted, accessing https://mindreader.web.ctfcompetition.com/?f=/dev/fd/../environ gets us our flag:

![Imgur](http://i.imgur.com/533UYyz.png)

`CTF{ee02d9243ed6dfcf83b8d520af8502e1}`

Thanks Google for the interesting question! I definitely learned a lot about the Linux filesystem through this.
