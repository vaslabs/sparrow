# Sparrow is a short url service

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


To generate a universal package do sbt universal:packageBin

