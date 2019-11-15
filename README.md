# Spark Quickbooks Connector

Build status: [![Build Status](https://travis-ci.com/inetsoft-technology/spark-quickbooks.svg?branch=master)][1]

Spark SQL 2.3.2 Connector for QuickBooks Online

## Requirements

QuickBooks Online uses OAuth 2.0 for authorization to query their API. You will need to
create keys as described in the [Authentication and Authorization Guide][2]

If the account is set up correctly you will be able to generate tokens and make API calls in the
[OAuth 2.0 Playground][3]

Note that the authorization code is one time use so if you need new tokens you will need to first
get a new authorization code.

The data source is implemented as `DataSourceV2` which cannot be accessed through the SQL syntax
in Spark 2.3.2. You will need to use the Java or Scala API to use this connector.

## Installation

Add the JARs as dependencies

#### Spark Data Source
```
groupId: com.inetsoft.connectors
artifactId: spark-quickbooks
version: 1.1.0
classifier: bundle
```

#### QuickBooks API
```
groupId: com.inetsoft.connectors
artifactId: spark-quickbooks-api
version: 1.1.0
```

## Options

| Option            | Description                       |
| ----------------- |---------------------------------- |
| authorizationCode | OAuth Authorization Code          |
| companyId         | QuickBooks Online Company ID      |
| clientId          | OAuth Client ID                   |
| clientSecret      | OAuthClientSecret                 |
| redirectUri       | HTTPS endpoint for OAuth redirect |
| entity            | Object to query from the API      |
| production        | Query production environment      |
| expandArrays      | Expands nested arrays to columns  |

* `authorizationCode`: Exchanged for access/refresh tokens
* `companyId`: Also called `realmId`, it's the ID of the company that you want to query in QuickBooks
* `clientId`: From your app's **Keys** tab
* `clientSecret`: From your app's **Keys** tab
* `redirectUri`: Also in the **Keys** tab, this need to be HTTPS in production and cannot be localhost
  * Default: `https://developer.intuit.com/v2/OAuth2Playground/RedirectUrl`
* `entity`: Due to the nature of the QuickBooks Online query syntax, only 1 entity may be queried at a time.
* `production`: set to `true` when switching from a sandbox to production environment
* `expandArrays`: `true` to expand every element in an array to its own column
    * `lineItems: [{price: 7.0}, {price: 3.0}]` becomes `lineItems_0_price, lineItems_1_price`
    with the value 7.0 and 3.0 respectively

We take this entity and pass it as the query `select * from <entity>` and then create a data frame
from the result set to query against with Spark SQL

[1]:https://travis-ci.com/inetsoft-technology/spark-quickbooks
[2]:https://developer.intuit.com/app/developer/qbo/docs/develop/authentication-and-authorization/oauth-2.0#obtain-oauth2-credentials-for-your-app
[3]:https://developer.intuit.com/app/developer/playground
