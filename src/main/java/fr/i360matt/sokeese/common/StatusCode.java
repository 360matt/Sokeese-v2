package fr.i360matt.sokeese.common;

public enum StatusCode {
    OK (0),
    ALREADY_LOGGED (-1_2),
    CREDENTIALS (-1_1),
    OTHER (-1);



    StatusCode (int code) {
        this.code = code;
    };

    int code;
    public int getCode () {
        return code;
    }
}
