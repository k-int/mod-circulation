# mod-circulation

Copyright (C) 2017-2018 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Documentation

Further documentation about this module can be found in the
[Guide](doc/guide.md) file within the `/doc/` folder.

## Goal

FOLIO compatible circulation capabilities, including loan items from the inventory.

## Prerequisites

### Required

- Java 8 JDK
- Maven 3.5.0
- Implementations of the interfaces described in the [module descriptor](descriptors/ModuleDescriptor-template.json)

### Optional

- Python 3 (for un-registering module during managed demo and tests via Okapi, and the lint-raml tools)

## Preparation

### Git Submodules

There are some common RAML definitions that are shared between FOLIO projects via Git submodules.

To initialise these please run `git submodule init && git submodule update` in the root directory.

If these are not initialised, the inventory-storage module will fail to build correctly, and other operations may also fail.

More information is available on the [developer site](https://dev.folio.org/guides/developer-setup/#update-git-submodules).

## Common activities

### Running a general build

In order to run a general build (including the default tests), run `mvn test`.

### Creating the circulation module JAR

In order to build an executable Jar (e.g. for Okapi to deploy), run `mvn package`.

### Running the tests

#### Using fake modules

In order to run the tests, using a fake loan storage module, run ./quick-test.sh.

#### Using real modules (via Okapi)

In order to run the tests against a real storage module, run ./test-via-okapi.sh.

This requires [Okapi](https://github.com/folio-org/okapi) to be running and the relevant modules to be registered with it.

The test script will create a tenant and activate the modules for that tenant.

In order to change the specific versions of these dependencies, edit the test-via-okapi.sh script.

### Checking the RAML and JSON.Schema definitions

Follow the [guide](https://dev.folio.org/guides/raml-cop/) to use raml-cop to assess RAML, schema, and examples.

## Design Notes

### Known Limitations

#### Requests Created out of Request Date Order

Requests are assigned a position based upon when they were created.
This means the requests could be in a different position in the queue than what
the request date suggests. We could re-order to queue based upon request date
each time it is changed, however this would impede the future requirement
for the ability to reorder the queue manually.

#### Creating an already closed loan

It is not possible to create a loan that is already closed via POST
due to checks that are performed during this request. 
However it can be done when creating a loan in a specific location via PUT 

### Check Out By Barcode

In additional to the typical loan creation API, it is possible to check out an item to a loanee (optionally via a proxy), using barcodes.

#### Example Request

```
POST http://{okapi-location}/circulation/check-out-by-barcode
{
    "itemBarcode": "036000291452",
    "userBarcode": "5694596854",
    "loanDate": "2018-03-18T11:43:54.000Z"
}
```

#### Example Success Response

```
HTTP/1.1 201 Created
content-type: application/json; charset=utf-8
content-length: 1095
location: /circulation/loans/e01ed4d3-28c4-4f9b-89a2-e818e0a6e7f5

{
    "id": "e01ed4d3-28c4-4f9b-89a2-e818e0a6e7f5",
    "status": {
        "name": "Open"
    },
    "action": "checkedout",
    "loanDate": "2018-03-18T11:43:54.000Z",
    "userId": "2f400401-a751-456a-9f57-415cbce65864",
    "itemId": "8722fa77-dc6b-4182-9ea0-e3b708bee0f5",
    "dueDate": "2018-04-08T11:43:54.000Z",
    "loanPolicyId": "af30cbee-5d54-4a83-842b-eaef0f02cfbe",
    "metadata": {
        "createdDate": "2018-04-25T18:17:49.545Z",
        "createdByUserId": "79ff2a8b-d9c3-5b39-ad4a-0a84025ab085",
        "updatedDate": "2018-04-25T18:17:49.545Z",
        "updatedByUserId": "79ff2a8b-d9c3-5b39-ad4a-0a84025ab085"
    },
    "item": {
        "title": "The Long Way to a Small, Angry Planet",
        "contributors": [
            {
                "name": "Chambers, Becky"
            }
        ],
        "barcode": "036000291452",
        "holdingsRecordId": "e2309cd0-5b99-4bf2-8620-df7ac8edd38a",
        "instanceId": "ffe94513-fd4a-4c17-86ae-bfc936b47c06",
        "callNumber": "123456",
        "status": {
            "name": "Checked out"
        },
        "location": {
            "name": "3rd Floor"
        },
        "materialType": {
            "name": "Book"
        }
    }
}
```

#### Example Failure Response

Below is an example of a failure response.

The message explains the reason for the refusal of the request.

The parameters refer to what part of the request caused the request to be refused.

```
HTTP/1.1 422 Unprocessable Entity
content-type: application/json; charset=utf-8
content-length: 200

{
    "errors": [
        {
            "message": "Cannot check out item via proxy when relationship is invalid",
            "parameters": [
                {
                    "key": "proxyUserBarcode",
                    "value": "6430530304"
                }
            ]
        }
    ]
}
```

#### Validation

Below is a short summary summary of most of the validation checks performed when using this endpoint.

Each includes an example of the error message provided and the parameter key included with the error.

|Check|Example Message|Parameter Key|Notes|
|---|---|---|---|
|Item does not exist|No item with barcode 036000291452 exists|itemBarcode| |
|Holding does not exist| | |otherwise it is not possible to lookup loan rules|
|Item is already checked out|Item is already checked out|itemBarcode| |
|Existing open loan for item|Cannot check out item that already has an open loan|itemBarcode| |
|Proxy relationship is valid|Cannot check out item via proxy when relationship is invalid| |only if proxying|
|User must be requesting user|User checking out must be requester awaiting pickup|userBarcode|if there is an outstanding fulfillable request for item|
|User does not exist|Could not find user with matching barcode|userBarcode| |
|User needs to be active and not expired|Cannot check out to inactive user|userBarcode| |
|Proxy user needs to be active and not expired|Cannot check out via inactive proxying user|proxyUserBarcode|only if proxying|

### Renew By Barcode

It is possible to renew an item to a loanee (optionally via a proxy), using barcodes for the item and loanee.

#### Example Request

```
POST http://localhost:9605/circulation/renew-by-barcode
{
    "itemBarcode": "036000291452",
    "userBarcode": "5694596854"
}
```

#### Example Success Response

```
HTTP/1.1 200 OK
content-type: application/json; charset=utf-8
content-length: 1114
location: /circulation/loans/a2494e15-cecf-4f68-a5bf-701389b278ed

{
    "id": "a2494e15-cecf-4f68-a5bf-701389b278ed",
    "status": {
        "name": "Open"
    },
    "action": "renewed",
    "loanDate": "2018-03-04T11:43:54.000Z",
    "userId": "891fa646-a46e-4152-9989-efe3b0311e04",
    "itemId": "9edc9877-df4c-4dd8-9306-3f1d1444c3f8",
    "dueDate": "2018-03-31T23:59:59.000Z",
    "loanPolicyId": "750fd537-3438-4f06-b854-94a1c34d199d",
    "renewalCount": 1,
    "metadata": {
        "createdDate": "2018-06-08T13:35:39.097Z",
        "createdByUserId": "79ff2a8b-d9c3-5b39-ad4a-0a84025ab085",
        "updatedDate": "2018-06-08T13:35:39.162Z",
        "updatedByUserId": "79ff2a8b-d9c3-5b39-ad4a-0a84025ab085"
    },
    "item": {
        "holdingsRecordId": "092c24f3-44e8-44a5-9389-5b3f50e4895a",
        "instanceId": "a1039600-2076-4248-af75-6b561fdb0f09",
        "title": "The Long Way to a Small, Angry Planet",
        "barcode": "036000291452",
        "contributors": [
            {
                "name": "Chambers, Becky"
            }
        ],
        "callNumber": "123456",
        "status": {
            "name": "Checked out"
        },
        "location": {
            "name": "3rd Floor"
        },
        "materialType": {
            "name": "Book"
        }
    }
}
```

#### Example Failure Response

Below is an example of a failure response.

The message explains the reason for the refusal of the request.

The parameters refer to what part of the request caused the request to be refused.

```
HTTP/1.1 422 Unprocessable Entity
content-type: application/json; charset=utf-8
content-length: 611

{
    "errors": [
        {
            "message": "renewal at this time would not change the due date",
            "parameters": [
                {
                    "key": "loanPolicyName",
                    "value": "Limited Renewals And Limited Due Date Policy"
                },
                {
                    "key": "loanPolicyId",
                    "value": "9b28ec73-0582-4751-bd5c-65c03965ae65"
                }
            ]
        },
        {
            "message": "loan has reached it's maximum number of renewals",
            "parameters": [
                {
                    "key": "loanPolicyName",
                    "value": "Limited Renewals And Limited Due Date Policy"
                },
                {
                    "key": "loanPolicyId",
                    "value": "9b28ec73-0582-4751-bd5c-65c03965ae65"
                }
            ]
        }
    ]
}
```

### Loan Rules Caching

The loan rules engine used for applying loan rules has an internal, local cache which is refreshed every 5 seconds and
when a PUT to /circulation/loan-rules changes the loan rules.

This is per module instance, and so may result in different responses during this window after the loan rules are changed.

### Loan Rules

[doc/loanrules.md](doc/loanrules.md)

That document explains how the loan rules engine calculates the loan policy (that specifies the loan period)
based on the patron's patron group and the item's material type, loan type, and location.

### Item Status

During the circulation process an item can change between a variety of states,
below is a table describing the most common states defined at the moment.

| Name | Description |
|---|---|
| Available | This item is available to be lent to a patron |
| Checked out | This item is currently checked out to a patron |
| Awaiting pickup | This item is awaiting pickup by a patron who has a request at the top of the queue|

### Request Status

| Name | Description |
|---|---|
| Open - Not yet filled | The requested item is not yet available to the requesting user |
| Open - Awaiting pickup | The item is available to the requesting user |
| Closed - Filled | |

### Storing Information from Other Records

In order to facilitate the searching and sorting of requests by the properties of related records, a snapshot of some
properties are stored with the request.

This snapshot is updated during POST or PUT requests by requesting the current state of those records.
It is possible for them to become out of sync with the referenced records.

the request JSON.schema uses the readOnly property to indicate that these properties, from the perspective of the client, are read only.

#### Properties Stored

##### Requesting User (referenced by requesterId, held in requester property)

* firstName
* lastName
* middleName
* barcode

##### Proxy Requesting User (referenced by proxyUserId, held in proxy property)

* firstName
* lastName
* middleName
* barcode

##### Requested Item (referenced by itemId, held in item property)

* title
* barcode

### Including Properties From Other Records

In order to reduce the amount of requests a client needs to make, some properties from other records in responses.

As this inclusion requires a chain of requests after the loans or requests have been fetched, the responses may take longer than other requests.

#### Loans

Loans include information from the item, including locations, holdingsRecordId and instanceId.

#### Requests

Requests include information from the item, including holdingsRecordId and instanceId.

## Additional Information

Other [modules](https://dev.folio.org/source-code/#server-side).

See project [CIRC](https://issues.folio.org/browse/CIRC)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker/).

Other FOLIO Developer documentation is at [dev.folio.org](https://dev.folio.org/)

