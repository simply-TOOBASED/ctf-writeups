Writeup for mindreader

At first look at mindreader, just putting in random strings seems to be returning a 404 saying "URL not found". 

So what happens if we an actual URL? Trying http://www.google.com just returns the same 404. However, what happens if we try a file on the website? (hence the "what do you want to read?" part - it's asking what file we want to read) Trying obvious file names (index.php, index.html, etc.), and we see that "index.html" as an input string seems to work, so that confirms that the website is a file reader.

Next step is to try and guess what kind of web server or framework the website is running, i.e. something like Flask, Apache, NodeJS, etc. "requirements.txt" seems to work, and tells us that the website is running off of Flask, a popular python web framework. Flask runs off of an app.py or a main.py, normally, so we try both of those filenames, and we find a main.py file!
        
As we can see, the flag is an environment variable, after some googling we see that we have to access "/proc/self/environ". However, because of the re.search, we can't have "proc" in our string at all.

After being stuck for a couple of hours, my teammate gave me some useful information: "/dev/fd" is a symlink to "/proc/self/fd". Therefore we can just just make our filename "/dev/fd/../environ", which is the equivalent of "/proc/self/environ" and we get the flag!

Accessing https://mindreader.web.ctfcompetition.com/?f=/dev/fd/../environ gets us:

CTF{ee02d9243ed6dfcf83b8d520af8502e1}
