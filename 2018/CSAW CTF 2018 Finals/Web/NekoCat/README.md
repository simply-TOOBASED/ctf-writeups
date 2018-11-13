> Flag is in /flag.txt
> 
> http://web.chal.csaw.io:1003
> 
> Files
> 
> [flagon.zip](https://ctf.csaw.io/files/5d0977e48372db7e81cbf84752a4c511/flagon.zip)

The website is similar to twitter, where users can create and send posts about whatever they want, and everyone else can see them in one complete feed. 

![Imgur](https://i.imgur.com/KoROMvT.png)

Looking at the zip, we see it's the complete source code of the website, and that it's running the Flask web framework. We see some interesting information in flagon.py:

```python
SECRET_KEY = os.environ.get("FLAGON_SECRET_KEY", "")

...

class Request(BaseRequest):
    @cached_property
    def session(self):
        data = self.cookies.get("session_data")
        if not data:
            return SecureCookie(secret_key=SECRET_KEY)
        return SecureCookie.unserialize(data, SECRET_KEY)


def flagoninfo(request):
    if request.remote_addr != "127.0.0.1":
        return render_template("404.html")

    info = {
        "system": " ".join(os.uname()),
        "env": str(os.environ)
    }

    return render_template("flaginfo.html", info_dict=info)
```
The application has a SECRET_KEY that it uses to create SecureCookie's. The first thing we need to do is figure out how to get the SECRET_KEY so we can sign our own cookies. Once we can sign our own cookies, we can utilize an RCE to get the flag. Because SecureCookie uses [pickle](https://docs.python.org/3/library/pickle.html) by default for serialization, we can exploit the deserialization to execute python code. For more information about python pickle deserialization vulnerabilities, you can visit this [link](https://crowdshield.com/blog.php?name=exploiting-python-deserialization-vulnerabilities).
