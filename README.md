# Sparrow is a short url service

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/08b69f198c26483ca72e98fb1dd5837d)](https://app.codacy.com/app/vaslabs/sparrow?utm_source=github.com&utm_medium=referral&utm_content=vaslabs/sparrow&utm_campaign=Badge_Grade_Dashboard)

## Dependencies
Sparrow uses dynamoDB for persistent storage. To use locally do
```
docker compose up
./local_dynamo_setup.sh
```

Then you can run the application or execute the test suite.

## Deployment

If you want to deploy it on your aws account you need to change or override the reference.conf configuration to not point to the localhost.
Then create a dynamodb table with the same properties as used in the local_dynamo_setup.sh

In production set the environment variable X_SPARROW_AUTH to a secret value. It will be used for authenticating the creation of short urls and stats retrieval.

To generate a universal package do `sbt universal:packageBin`

## Usage

### To create a short url

```
curl -X POST \
  http://localhost:8080/entry \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'x_sparrow_auth: 0000000000000000' \
  -d '{
	"url": "http://blog.vaslabs.org/2017/07/the-need-for-immutability.html"
}'
```

You can also request your own short url id like this
```
curl -X POST \
  http://localhost:8080/entry \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'x_sparrow_auth: 0000000000000000' \
  -d '{
	"url": "http://blog.vaslabs.org/2017/07/the-need-for-immutability.html",
	"customShortKey": "immutables"
}'
```

You'll get back a response of 4 characters or your custom sequence. If you then visit http://localhost:8080/<thefourcharacters> you should be redirected to the url.

### To view stats

```
curl -X GET \
  http://localhost:8080/stats \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'x_sparrow_auth: 0000000000000000'
```

