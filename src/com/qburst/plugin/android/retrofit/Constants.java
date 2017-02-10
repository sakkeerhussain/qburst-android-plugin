package com.qburst.plugin.android.retrofit;

/**
 * Created by sakkeer on 18/01/17.
 */
public class Constants {
    public static final String STRING_MODEL = "Model";
    public static final String STRING_REQUEST_MODEL = "RequestModel";
    public static final String STRING_RESPONSE_MODEL = "ResponseModel";

    public static final String PACKAGE_NAME_RETROFIT = "com.qburst.retrofit";
    public static final String PACKAGE_NAME_RETROFIT_REQUEST = "com.qburst.retrofit.model.request";
    public static final String PACKAGE_NAME_RETROFIT_RESPONSE = "com.qburst.retrofit.model.response";

    public static final String DEPENDENCY_RETROFIT = "com.squareup.retrofit2:retrofit:2.1.0";
    public static final String DEPENDENCY_RETROFIT_GSON = "com.squareup.retrofit2:converter-gson:2.1.0";
    public static final String DEPENDENCY_RETROFIT_LOGGING = "com.squareup.okhttp3:logging-interceptor:3.2.0";

    public static final String BASE_URL_FIELD = "private static final String BASE_URL = \"%s\";";
    public static final String GET_INSTANCE_METHOD = "public static APIService getInstance(final android.content.Context context) {\n" +
            "\n" +
            "        APIService service;\n" +
            "\n" +
            "        okhttp3.logging.HttpLoggingInterceptor loggingInterceptor = new okhttp3.logging.HttpLoggingInterceptor();\n" +
            "        loggingInterceptor.setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BODY);\n" +
            "        okhttp3.Interceptor headerInterceptor = new okhttp3.Interceptor() {\n" +
            "            @Override\n" +
            "            public okhttp3.Response intercept(Chain chain) throws java.io.IOException {\n" +
            "                okhttp3.Request.Builder requestBuilder = chain.request().newBuilder()\n" +
            "                        .addHeader(\"Content-Type\", \"Application/json\");\n" +
            "                return chain.proceed(requestBuilder.build());\n" +
            "            }\n" +
            "        };\n" +
            "\n" +
            "        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()\n" +
            "                .addInterceptor(headerInterceptor)\n" +
            "                .addInterceptor(loggingInterceptor)\n" +
            "                .readTimeout(2, java.util.concurrent.TimeUnit.MINUTES)\n" +
            "                .writeTimeout(2, java.util.concurrent.TimeUnit.MINUTES)\n" +
            "                .build();\n" +
            "\n" +
            "        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()\n" +
            "                .baseUrl(BASE_URL)\n" +
            "                .client(client)\n" +
            "                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())\n" +
            "                .build();\n" +
            "\n" +
            "        service = retrofit.create(APIService.class);\n" +
            "\n" +
            "        return service;\n" +
            "    }";

    public class ServiceInterface {
        public static final String ANNOTATION_FORMAT = "@retrofit2.http.%s(\"%s\")";
        public static final String METHOD = "%s \n retrofit2.Call<%s> %s(%s);";
        public static final String REQUEST_PARAM_QUERY = "@retrofit2.http.Query(\"%s\") %s %s, ";
        public static final String REQUEST_PARAM_BODY = "@retrofit2.http.Body %s %s, ";
    }

    public class className{
        public static final String MANAGER = "RetrofitManager";
        public static final String SERVICE = "APIService";
    }

    public class RegExp{
        public static final String END_POINT_URL_PARAMS = "\\?(([a-zA-Z][a-zA-Z0-9_]*)=[^&=]+(&([a-zA-Z][a-zA-Z0-9_]*)=[^&=]+)*)?";
        public static final String END_POINT_URL_FIRST = "([a-zA-Z_/0-9]*((\\{{1})([a-zA-Z][a-zA-Z0-9_]*)=[^}]+(}{1})*)*)*";
        public static final String END_POINT_URL = END_POINT_URL_FIRST + END_POINT_URL_PARAMS;
    }
}
