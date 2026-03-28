package org.example.lsw.client.api;

//for team:
//the tutorial I read said ApiException is good practice for decoupling so that's why it exists
//Before submission make sure to replace all direct exception handling in the scenes with this ApiException
public class ApiException extends RuntimeException {
    public ApiException(String message) {super(message);}
}
