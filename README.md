CSYE-6225 Network Structures and Cloud Computing

Technology Stack
1. Spring Boot
2. Spring Security
3. Spring JPA
4. MYSQL
5. Junit
6. Mockito
7. Maven

Pre-Requisites

1. Install Spring Tool Suite (STS)
2. Install Postman
3. Install MYSQL

Build Instructions

1. Clone/Download the project on your local
2. Open your project in any IDE 
3. Right click on the main project -> Maven -> Update Project
4. In the resources -> application.properties file, change the MYSQL username and password
5. Right click on the main project -> Run As -> Spring Boot App
6. You can validate your REST Endpoints using Postman
7. To validate User Endpoints:
   a. POST = "localhost:8080/v1/user"
      Request Header : Content-Type -> application/json
      Request Body: raw 
   b. GET = "localhost:8080/v1/user/self"
      Authorization : Basic Auth
      Request Header: Content-Type -> application/json
   c. PUT = "localhost:8080/v1/user/self"
      Authorization : Basic Auth
      Request Header: Content-Type -> application/json 
8. To validate Bill Endpoints:
   a. POST = "localhost:8080/v1/bill/"
      Request Header : Content-Type -> application/json
      Request Body: raw 
      Authorization : Basic Auth
   b. GET = "localhost:8080/v1/bills"
      Request Header : Content-Type -> application/json
      Authorization : Basic Auth 
   c. GET = "localhost:8080/v1/bill/{id}"
      Request Header : Content-Type -> application/json
      Authorization : Basic Auth
   d. PUT = "localhost:8080/v1/bill/{id}"
      Request Header : Content-Type -> application/json
      Request Body: raw 
      Authorization : Basic Auth
   e. POST = "localhost:8080/v1/bill/{id}"
      Request Header : Content-Type -> application/json
      Authorization : Basic Auth
9. To validate File Endpoints:
   a. POST = "localhost:8080/v1/bill/{id}/file"
      Request Body: form-data
      Authorization : Basic Auth
   b. GET = "localhost:8080//v1/bill/{billId}/file/{fileId}"
      Request Header : Content-Type -> application/json
      Authorization : Basic Auth
   c. DELETE = "localhost:8080//v1/bill/{billId}/file/{fileId}"
      Request Header : Content-Type -> application/json
      Authorization : Basic Auth

Deploy Instructions

Commits and Merge to the git repository invokes the CI/CD build-deploy job

Test Instructions

Right click on the WebAppRestLayerUnitTest under the test/java/com/csye6225/controllertest and Run As -> JUnit Test


CI/CD

CircleCI used for continuous integration and continuous deployment

Codedeploy runs with the merge request success


