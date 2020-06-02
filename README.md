
# Undertow not handling GET - Body as null

Problem: Undertow returning Null in Body of Get
Investigate: In below class of undertow component, we could see for GET method is not handled so body is coming as null. 

Solution: Either you Jetty component or modify the class

https://github.com/apache/camel/blob/camel-2.23.x/components/camel-undertow/src/main/java/org/apache/camel/component/undertow/DefaultUndertowHttpBinding.java

        } else {

            //extract body by myself if undertow parser didn't handle and the method is allowed to have one

            //body is extracted as byte[] then auto TypeConverter kicks in

            if (Methods.POST.equals(httpExchange.getRequestMethod()) || Methods.PUT.equals(httpExchange.getRequestMethod()) || Methods.PATCH.equals(httpExchange.getRequestMethod())) {

                result.setBody(readFromChannel(httpExchange.getRequestChannel()));

            } else {

                result.setBody(null);

            }
