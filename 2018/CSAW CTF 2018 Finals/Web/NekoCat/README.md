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

    def wsgi_app(self, environ, start_response):
        request = Request(environ)
        response = self.dispatch_request(request)
        if request.session.should_save:
            session_data = request.session.serialize()
            response.set_cookie('session_data', session_data,
                                httponly=True)
        return response(environ, start_response)

    def route(self, route):
        def decorator(f):
            @wraps(f)
            def new_retval(*args, **kwargs):
                ret = f(*args)
                if type(ret) != Response:
                    ret = Response(ret)
                return ret

            self.routes[route] = new_retval
            self.url_map.add(Rule(route, endpoint=route))

            return new_retval
        return decorator

    def dispatch_request(self, request):
        adapter = self.url_map.bind_to_environ(request.environ)

        try:
            endpoint, values = adapter.match()
            matched_route = self.routes[endpoint]
            return matched_route(request, **values)
        except NotFound as e:
            return render_template('404.html')
        except HTTPException as e:
            return e

    def __call__(self, environ, start_response):
        return self.wsgi_app(environ, start_response)

    def run(self, host, port=5000):
        from werkzeug.serving import run_simple
        run_simple(host, port, self, use_reloader=True,
                   processes=40)
```
The application has a SECRET_KEY that it uses to create SecureCookie's. The first thing we need to do is figure out how to get the SECRET_KEY so we can sign our own cookies. Once we can sign our own cookies, we can utilize an RCE to get the flag. Because SecureCookie uses [pickle](https://docs.python.org/3/library/pickle.html) by default for serialization, we can exploit the deserialization to execute python code. For more information about python pickle deserialization vulnerabilities, you can visit this [link](https://crowdshield.com/blog.php?name=exploiting-python-deserialization-vulnerabilities).

Trying to directly access http://web.chal.csaw.io:1003/flaginfo returns a 404, which makes sense as our ip address 
