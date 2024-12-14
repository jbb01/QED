# Add project specific ProGuard rules here.
-dontobfuscate
-dontoptimize

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn **com.fasterxml.jackson.core.JsonParser$Feature
-dontwarn com.fasterxml.jackson.core.JsonProcessingException
-dontwarn com.fasterxml.jackson.core.type.TypeReference
-dontwarn com.fasterxml.jackson.databind.JsonNode
-dontwarn com.fasterxml.jackson.databind.ObjectMapper
-dontwarn com.madrobot.beans.BeanInfo
-dontwarn com.madrobot.beans.IntrospectionException
-dontwarn com.madrobot.beans.Introspector
-dontwarn com.madrobot.beans.PropertyDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn net.minidev.json.JSONArray
-dontwarn net.minidev.json.JSONValue
-dontwarn net.minidev.json.parser.ContainerFactory
-dontwarn net.minidev.json.parser.JSONParser
-dontwarn org.cheffo.jeplite.JEP
-dontwarn org.cheffo.jeplite.ParseException
-dontwarn sun.misc.BASE64Encoder