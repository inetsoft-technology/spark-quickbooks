# Spark Quickbooks Connector

Build status: [![Build Status](https://travis-ci.com/inetsoft-technology/spark-quickbooks.svg?branch=master)][1]

Spark SQL 3.1.1 Connector for QuickBooks Online

## Requirements

QuickBooks Online uses OAuth 2.0 for authorization to query their API. You will need to
create keys as described in the [Authentication and Authorization Guide][2]

If the account is set up correctly you will be able to generate tokens and make API calls in the
[OAuth 2.0 Playground][3]

Note that the authorization code is one time use so if you need new tokens you will need to first
get a new authorization code.

If you have already generated an access token you can pass that as an option instead of having
the data source manage the OAuth flow.

## Installation

Add the JARs as dependencies

#### Spark Data Source
```
groupId: com.inetsoft.connectors
artifactId: spark-quickbooks
version: 2.0.2
classifier: bundle
```

#### QuickBooks API
```
groupId: com.inetsoft.connectors
artifactId: spark-quickbooks-api
version: 2.0.2
```

## General Options

| Option            | Description                       |
| ----------------- |---------------------------------- |
| companyId         | QuickBooks Online Company ID      |
| entity            | Object to query from the API      |
| production        | Query production environment      |
| expandArrays      | Expands nested arrays to columns  |
| expandStructs     | Expands nested structs to columns |

* `companyId`: Also called `realmId`, it's the ID of the company that you want to query in QuickBooks
* `entity`: Due to the nature of the QuickBooks Online query syntax, only 1 entity may be queried at a time.
* `production`: set to `true` when switching from a sandbox to production environment
* `expandArrays`: `true` to expand every element in an array to its own column
    * `lineItems: [{price: 7.0}, {price: 3.0}]` becomes `lineItems_0_price, lineItems_1_price`
    with the value 7.0 and 3.0 respectively
* `expandStructs`: default `true` to expand nested structs to their own columns

## OAuth Options

| Option            | Description                       |
| ----------------- |---------------------------------- |
| accessToken       | OAuth Access Token                |
| authorizationCode | OAuth Authorization Code          |
| clientId          | OAuth Client ID                   |
| clientSecret      | OAuthClientSecret                 |
| redirectUri       | HTTPS endpoint for OAuth redirect |

* `accessToken`: OAuth access token if you have already generated one. If you pass an access token
you don't need to pass any other OAuth options and we'll try to use your token instead of going
through the authorization process.
* `authorizationCode`: Exchanged for access/refresh tokens
* `clientId`: From your app's **Keys** tab
* `clientSecret`: From your app's **Keys** tab
* `redirectUri`: Also in the **Keys** tab, this need to be HTTPS in production and cannot be localhost
  * Default: `https://developer.intuit.com/v2/OAuth2Playground/RedirectUrl`

We take this entity and pass it as the query `select * from <entity>` and then create a data frame
from the result set to query against with Spark SQL

[1]:https://travis-ci.com/inetsoft-technology/spark-quickbooks
[2]:https://developer.intuit.com/app/developer/qbo/docs/develop/authentication-and-authorization/oauth-2.0#obtain-oauth2-credentials-for-your-app
[3]:https://developer.intuit.com/app/developer/playground
