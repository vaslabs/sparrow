#!/bin/bash
aws dynamodb --endpoint-url=http://0.0.0.0:4569 --region=eu-west-1 create-table --table-name url-shortener --attribute-definitions \
    AttributeName=shortVersion,AttributeType=S --key-schema AttributeName=shortVersion,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=1,WriteCapacityUnits=1