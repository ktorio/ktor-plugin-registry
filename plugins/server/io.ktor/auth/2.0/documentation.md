
Ktor provides the [Authentication](https://ktor.io/docs/authentication.html) plugin to handle authentication and authorization. Typical usage scenarios include logging in users, granting access to specific resources, and securely transmitting information between parties. You can also use `Authentication` with [Sessions](https://ktor.io/docs/sessions.html) to keep a user's information between requests.

## Usage

You can use the following authentications and authorization schemes:
* _HTTP authentication_ that includes the `Basic`, `Digest`, and `Bearer` schemes.
* _Form-based authentication_ that uses a web form to collect credential information and authenticate a user.
* _JSON Web Tokens (JWT)_ for securely transmitting information between parties as a JSON object.
* _LDAP_ for directory services authentication.
* _OAuth_ that allows you to implement authentication using external providers such as Google, Facebook, Twitter, and so on.
* _Session authentication_ that allows you to authenticate a user with already has an associated session.

You can learn more from the [Authentication](https://ktor.io/docs/authentication.html) section.
