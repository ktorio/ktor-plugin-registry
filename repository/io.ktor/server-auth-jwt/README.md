
JSON Web Token is an open standard that defines a way for securely transmitting information between parties as a JSON object. This information can be verified and trusted since it is signed using a shared secret (with the `HS256` algorithm) or a public/private key pair (for example, `RS256`).

Ktor handles JWTs passed in the `Authorization` header using the `Bearer` schema and allows you to:
* verify the signature of a JSON web token;
* perform additional validations on the JWT payload.

## Usage

To learn how to use JSON web tokens in a server Ktor application, see [JSON Web Tokens](https://ktor.io/docs/jwt.html).
