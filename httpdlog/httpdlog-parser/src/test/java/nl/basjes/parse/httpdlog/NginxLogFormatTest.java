/*
 * Apache HTTPD logparsing made easy
 * Copyright (C) 2011-2017 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.basjes.parse.httpdlog;

import nl.basjes.parse.core.Parser;
import nl.basjes.parse.core.exceptions.DissectionFailure;
import nl.basjes.parse.core.exceptions.InvalidDissectorException;
import nl.basjes.parse.core.exceptions.MissingDissectorsException;
import nl.basjes.parse.core.nl.basjes.parse.core.test.DissectorTester;
import nl.basjes.parse.core.nl.basjes.parse.core.test.TestRecord;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class NginxLogFormatTest {

    private static final Logger LOG = LoggerFactory.getLogger(NginxLogFormatTest.class);

    @Test
    public void testBasicLogFormat() {
        // From: http://articles.slicehost.com/2010/8/27/customizing-nginx-web-logs
        String logFormat = "$remote_addr - $remote_user [$time_local] \"$request\" $status $body_bytes_sent \"$http_referer\" \"$http_user_agent\"";
        String logLine = "123.65.150.10 - - [23/Aug/2010:03:50:59 +0000] \"POST /wordpress3/wp-admin/admin-ajax.php HTTP/1.1\" 200 2 \"http://www.example.com/wordpress3/wp-admin/post-new.php\" \"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_4; en-US) AppleWebKit/534.3 (KHTML, like Gecko) Chrome/6.0.472.25 Safari/534.3\"";

        DissectorTester.create()
            .verbose()
            .withParser(new HttpdLoglineParser<>(TestRecord.class, logFormat))
            .withInput(logLine)

            .printPossible()
            .printAllPossibleValues();
    }

    @Test
    public void testBasicLogFormatDissector() {
        // From: http://articles.slicehost.com/2010/8/27/customizing-nginx-web-logs
        String logFormat = "combined";
        String logLine = "123.65.150.10 - - [23/Aug/2010:03:50:59 +0000] \"POST /wordpress3/wp-admin/admin-ajax.php HTTP/1.1\" 200 2 \"http://www.example.com/wordpress3/wp-admin/post-new.php\" \"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_4; en-US) AppleWebKit/534.3 (KHTML, like Gecko) Chrome/6.0.472.25 Safari/534.3\"";

        HttpdLoglineParser<TestRecord> parser = new HttpdLoglineParser<>(TestRecord.class, "$msec");
        parser.dropDissector(HttpdLogFormatDissector.class);

        NginxHttpdLogFormatDissector dissector = new NginxHttpdLogFormatDissector();
        dissector.setLogFormat(logFormat);
        parser.addDissector(dissector);

        DissectorTester.create()
            .verbose()
            .withParser(parser)
            .withInput(logLine)
            .printPossible()
            .printAllPossibleValues();
    }

    @Test
    public void testCompareApacheAndNginxOutput() throws NoSuchMethodException, InvalidDissectorException, MissingDissectorsException, DissectionFailure {

        String logFormatNginx  = "$remote_addr - $remote_user [$time_local] \"$request\" $status $body_bytes_sent \"$http_referer\" \"$http_user_agent\"";

        // combined format except the logname was removed.
        String logFormatApache = "%h - %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"";

        String logLine = "1.2.3.4 - - [23/Aug/2010:03:50:59 +0000] \"POST /foo.html?aap&noot=mies HTTP/1.1\" 200 2 \"http://www.example.com/bar.html?wim&zus=jet\" \"Niels Basjes/1.0\"";

        Parser<TestRecord> apacheParser = new HttpdLoglineParser<>(TestRecord.class, logFormatApache);
        List<String> fields = apacheParser.getPossiblePaths();

        ArrayList<String> checkFields = new ArrayList<>(fields.size() + 10);
        String[] parameterNames = { "aap", "noot", "mies", "wim", "zus", "jet" };
        for (String field: fields) {
            if (field.endsWith(".*")){
                String fieldHead = field.substring(0, field.length()-1);
                for (String parameterName: parameterNames){
                    checkFields.add(fieldHead + parameterName);
                }
            } else {
                checkFields.add(field);
            }
        }

        Parser<TestRecord> nginxParser = new HttpdLoglineParser<>(TestRecord.class, logFormatNginx);
        for (String field: checkFields) {
            apacheParser.addParseTarget(TestRecord.class.getMethod("setStringValue", String.class, String.class), field);
            nginxParser.addParseTarget(TestRecord.class.getMethod("setStringValue", String.class, String.class), field);
        }

        TestRecord apacheRecord = apacheParser.parse(logLine);
        TestRecord nginxRecord  = nginxParser.parse(logLine);

        for (String field: checkFields) {
            boolean apacheHasValue = apacheRecord.hasStringValue(field);
            boolean nginxHasValue = nginxRecord.hasStringValue(field);
            assertEquals("Apache and Nginx values for field " + field + " are different.", apacheHasValue, nginxHasValue);

            if (apacheRecord.hasStringValue(field)) {
                String apacheValue = apacheRecord.getStringValue(field);
                String nginxValue = nginxRecord.getStringValue(field);
                assertEquals("Apache and Nginx values for field " + field + " are different.", apacheValue, nginxValue);
            }

        }

    }


    @Test
    public void testFullTestAllFields() {
        String logFormat =
                "# \"$status\" " +
                "# \"$time_iso8601\" " +
                "# \"$time_local\" " +
                "# \"$arg_name\" " +
                "# \"$args\" " +
                "# \"$query_string\" " +
                "# \"$binary_remote_addr\" " +
                "# \"$body_bytes_sent\" " +
                "# \"$bytes_sent\" " +
                "# \"$connection\" " +
                "# \"$connection_requests\" " +
                "# \"$content_length\" " +
                "# \"$content_type\" " +
                "# \"$cookie_name\" " +
                "# \"$document_root\" " +
                "# \"$host\" " +
                "# \"$hostname\" " +
                "# \"$http_name\" " +
                "# \"$https\" " +
                "# \"$is_args\" " +
                "# \"$limit_rate\" " +
                "# \"$msec\" " +
                "# \"$nginx_version\" " +
                "# \"$pid\" " +
                "# \"$pipe\" " +
                "# \"$proxy_protocol_addr\" " +
                "# \"$realpath_root\" " +
                "# \"$remote_addr\" " +
                "# \"$remote_port\" " +
                "# \"$remote_user\" " +
                "# \"$request\" " +
                "# \"$request_body\" " +
                "# \"$request_body_file\" " +
                "# \"$request_completion\" " +
                "# \"$request_filename\" " +
                "# \"$request_length\" " +
                "# \"$request_method\" " +
                "# \"$request_time\" " +
                "# \"$request_uri\" " +
                "# \"$scheme\" " +
                "# \"$sent_http_etag\" " +
                "# \"$sent_http_last_modified\" " +
                "# \"$server_addr\" " +
                "# \"$server_name\" " +
                "# \"$server_port\" " +
                "# \"$server_protocol\" " +
                "# \"$tcpinfo_rtt\" " +
                "# \"$tcpinfo_rttvar\" " +
                "# \"$tcpinfo_snd_cwnd\" " +
                "# \"$tcpinfo_rcv_space\" " +
                "# \"$uri\" " +
                "# \"$document_uri\" " +
                "# \"$http_user_agent\" " +
                "# \"$http_referer\" " +
                "#";
        String logLine =
              /* $status                  */  "# \"200\" " +
              /* $time_iso8601            */  "# \"2017-01-03T15:56:36+01:00\" " +
              /* $time_local              */  "# \"03/Jan/2017:15:56:36 +0100\" " +
              /* $arg_name                */  "# \"-\" " +
              /* $args                    */  "# \"aap&noot=&mies=wim\" " +
              /* $query_string            */  "# \"aap&noot=&mies=wim\" " +
              /* $binary_remote_addr      */  "# \"\\x7F\\x00\\x00\\x01\" " +
              /* $body_bytes_sent         */  "# \"436\" " +
              /* $bytes_sent              */  "# \"694\" " +
              /* $connection              */  "# \"5\" " +
              /* $connection_requests     */  "# \"4\" " +
              /* $content_length          */  "# \"-\" " +
              /* $content_type            */  "# \"-\" " +
              /* $cookie_name             */  "# \"-\" " +
              /* $document_root           */  "# \"/var/www/html\" " +
              /* $host                    */  "# \"localhost\" " +
              /* $hostname                */  "# \"hackbox\" " +
              /* $http_name               */  "# \"-\" " +
              /* $https                   */  "# \"\" " +
              /* $is_args                 */  "# \"?\" " +
              /* $limit_rate              */  "# \"0\" " +
              /* $msec                    */  "# \"1483455396.639\" " +
              /* $nginx_version           */  "# \"1.10.0\" " +
              /* $pid                     */  "# \"5137\" " +
              /* $pipe                    */  "# \".\" " +
              /* $proxy_protocol_addr     */  "# \"\" " +
              /* $realpath_root           */  "# \"/var/www/html\" " +
              /* $remote_addr             */  "# \"127.0.0.1\" " +
              /* $remote_port             */  "# \"44448\" " +
              /* $remote_user             */  "# \"-\" " +
              /* $request                 */  "# \"GET /?aap&noot=&mies=wim HTTP/1.1\" " +
              /* $request_body            */  "# \"-\" " +
              /* $request_body_file       */  "# \"-\" " +
              /* $request_completion      */  "# \"OK\" " +
              /* $request_filename        */  "# \"/var/www/html/index.html\" " +
              /* $request_length          */  "# \"491\" " +
              /* $request_method          */  "# \"GET\" " +
              /* $request_time            */  "# \"0.000\" " +
              /* $request_uri             */  "# \"/?aap&noot=&mies=wim\" " +
              /* $scheme                  */  "# \"http\" " +
              /* $sent_http_etag          */  "# \"W/\\x22586bbb8b-29e\\x22\" " +
              /* $sent_http_last_modified */  "# \"Tue, 03 Jan 2017 14:56:11 GMT\" " +
              /* $server_addr             */  "# \"127.0.0.1\" " +
              /* $server_name             */  "# \"_\" " +
              /* $server_port             */  "# \"80\" " +
              /* $server_protocol         */  "# \"HTTP/1.1\" " +
              /* $tcpinfo_rtt             */  "# \"52\" " +
              /* $tcpinfo_rttvar          */  "# \"30\" " +
              /* $tcpinfo_snd_cwnd        */  "# \"10\" " +
              /* $tcpinfo_rcv_space       */  "# \"43690\" " +
              /* $uri                     */  "# \"/index.html\" " +
              /* $document_uri            */  "# \"/index.html\" " +
              /* $http_user_agent         */  "# \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.100 Safari/537.36\" " +
              /* $http_referer            */  "# \"http://localhost/\" " +
                                              "#";

        DissectorTester.create()
            .verbose()
            .withParser(new HttpdLoglineParser<>(TestRecord.class, logFormat))
            .withInput(logLine)

//            .printPossible()
            .printAllPossibleValues();
    }

    private class SingleFieldTestcase {
        String logformat;
        String logline;
        String fieldName;
        String expectedValue;

        public SingleFieldTestcase(String logformat, String logline, String fieldName, String expectedValue) {
            this.logformat = logformat;
            this.logline = logline;
            this.fieldName = fieldName;
            this.expectedValue = expectedValue;
        }
    }

    @Test
    public void validateAllFields() {
        List<SingleFieldTestcase> fieldsTests = new ArrayList<>();

        fieldsTests.add(new SingleFieldTestcase("$status"                  , "200"                                , "STRING:request.status.last"        , "200"                        ));
        fieldsTests.add(new SingleFieldTestcase("$time_iso8601"            , "2017-01-03T15:56:36+01:00"          , "TIME.ISO8601:request.receive.time" , "2017-01-03T15:56:36+01:00"  ));
        fieldsTests.add(new SingleFieldTestcase("$time_local"              , "03/Jan/2017:15:56:36 +0100"         , "TIME.STAMP:request.receive.time"   , "03/Jan/2017:15:56:36 +0100" ));

        fieldsTests.add(new SingleFieldTestcase("$time_iso8601"            , "2017-01-03T15:56:36+01:00"          , "TIME.EPOCH:request.receive.time.epoch"  , "1483455396000" ));
        fieldsTests.add(new SingleFieldTestcase("$time_local"              , "03/Jan/2017:15:56:36 +0100"         , "TIME.EPOCH:request.receive.time.epoch"  , "1483455396000" ));
        fieldsTests.add(new SingleFieldTestcase("$msec"                    , "1483455396.639"                     , "TIME.EPOCH:request.receive.time.epoch"  , "1483455396639" ));

        fieldsTests.add(new SingleFieldTestcase("$remote_addr"             , "127.0.0.1"                          , "IP:connection.client.host"           , "127.0.0.1"            ));
        fieldsTests.add(new SingleFieldTestcase("$binary_remote_addr"      , "\\x7F\\x00\\x00\\x01"               , "IP_BINARY:connection.client.host"    , "\\x7F\\x00\\x00\\x01" ));
        fieldsTests.add(new SingleFieldTestcase("$binary_remote_addr"      , "\\x7F\\x00\\x00\\x01"               , "IP:connection.client.host"           , "127.0.0.1"            ));

        fieldsTests.add(new SingleFieldTestcase("$remote_port"             , "44448"                              , "PORT:connection.client.port"       , "44448"    ));
        fieldsTests.add(new SingleFieldTestcase("$remote_user"             , "-"                                  , "STRING:connection.client.user"     , null       ));

        fieldsTests.add(new SingleFieldTestcase("$is_args"                 , "?"                                  , "STRING:request.firstline.uri.is_args"  , "?"                                   ));
        fieldsTests.add(new SingleFieldTestcase("$query_string"            , "aap&noot=&mies=wim"                 , "HTTP.QUERYSTRING:request.firstline.uri.query", "aap&noot=&mies=wim"   ));
        fieldsTests.add(new SingleFieldTestcase("$args"                    , "aap&noot=&mies=wim"                 , "HTTP.QUERYSTRING:request.firstline.uri.query", "aap&noot=&mies=wim"   ));
        fieldsTests.add(new SingleFieldTestcase("$args"                    , "aap&noot=&mies=wim"                 , "STRING:request.firstline.uri.query.aap",           ""      ));
        fieldsTests.add(new SingleFieldTestcase("$args"                    , "aap&noot=&mies=wim"                 , "STRING:request.firstline.uri.query.noot",          ""      ));
        fieldsTests.add(new SingleFieldTestcase("$args"                    , "aap&noot=&mies=wim"                 , "STRING:request.firstline.uri.query.mies",          "wim"   ));
        fieldsTests.add(new SingleFieldTestcase("$arg_name"                , "foo"                                , "STRING:request.firstline.uri.query.name",          "foo"   ));

        fieldsTests.add(new SingleFieldTestcase("$bytes_sent"              , "694"                                , "BYTES:response.bytes",             "694"   ));
        fieldsTests.add(new SingleFieldTestcase("$body_bytes_sent"         , "436"                                , "BYTES:response.body.bytes",        "436"   ));
        fieldsTests.add(new SingleFieldTestcase("$connection"              , "5"                                  , "NUMBER:connection.serial_number",  "5"     ));
        fieldsTests.add(new SingleFieldTestcase("$connection_requests"     , "4"                                  , "NUMBER:connection.requestnr",      "4"     ));
        fieldsTests.add(new SingleFieldTestcase("$https"                   , ""                                   , "STRING:connection.https"  ,        ""      ));
        fieldsTests.add(new SingleFieldTestcase("$content_length"          , "-"                                  , "HTTP.HEADER:request.header.content_length"                         ,    null         ));
        fieldsTests.add(new SingleFieldTestcase("$content_type"            , "-"                                  , "HTTP.HEADER:request.header.content_type"                           ,      null         ));
        fieldsTests.add(new SingleFieldTestcase("$cookie_name"             , "Something"                          , "HTTP.COOKIE:request.cookies.name"                          ,             "Something"  ));

        fieldsTests.add(new SingleFieldTestcase("$document_root"           , "/var/www/html"                      , "STRING:request.firstline.document_root"                    ,   "/var/www/html"  ));
        fieldsTests.add(new SingleFieldTestcase("$realpath_root"           , "/var/www/html"                      , "STRING:request.firstline.realpath_root"                    ,   "/var/www/html"  ));

        fieldsTests.add(new SingleFieldTestcase("$host"                    , "localhost"                          , "STRING:connection.server.name"                             ,   "localhost"      ));
        fieldsTests.add(new SingleFieldTestcase("$hostname"                , "hackbox"                            , "STRING:connection.client.host"                             ,   "hackbox"        ));
        fieldsTests.add(new SingleFieldTestcase("$http_name"               , "Something"                          , "HTTP.HEADER:request.header.name"                           ,   "Something"      ));
        fieldsTests.add(new SingleFieldTestcase("$nginx_version"           , "1.10.0"                             , "STRING:server.nginx.version"                               , "1.10.0"           ));
        fieldsTests.add(new SingleFieldTestcase("$pid"                     , "5137"                               , "NUMBER:connection.server.child.processid"                  , "5137"         ));
        fieldsTests.add(new SingleFieldTestcase("$pipe"                    , "."                                  , "STRING:connection.nginx.pipe"                              , "."                ));
        fieldsTests.add(new SingleFieldTestcase("$proxy_protocol_addr"     , ""                                   , "IP:connection.client.proxy.host"                           , ""                                    ));
        fieldsTests.add(new SingleFieldTestcase("$request"                 , "GET /?aap&noot=&mies=wim HTTP/1.1"  , "HTTP.FIRSTLINE:request.firstline"                          , "GET /?aap&noot=&mies=wim HTTP/1.1"   ));
        fieldsTests.add(new SingleFieldTestcase("$request_completion"      , "OK"                                 , "STRING:request.completion"                                 , "OK"                                  ));
        fieldsTests.add(new SingleFieldTestcase("$request_filename"        , "/var/www/html/index.html"           , "FILENAME:server.filename"                                  , "/var/www/html/index.html"            ));
        fieldsTests.add(new SingleFieldTestcase("$request_length"          , "491"                                , "BYTES:request.bytes"                                       , "491"                                 ));
        fieldsTests.add(new SingleFieldTestcase("$request_method"          , "GET"                                , "HTTP.METHOD:request.firstline.method"                      , "GET"                                 ));

        fieldsTests.add(new SingleFieldTestcase("$request_time"            , "123.456"                            , "SECOND_MILLIS:response.server.processing.time"             , "123.456"                             ));
        fieldsTests.add(new SingleFieldTestcase("$request_time"            , "123.456"                            , "MILLISECONDS:response.server.processing.time"              , "123456"                              ));
        fieldsTests.add(new SingleFieldTestcase("$request_time"            , "123.456"                            , "MICROSECONDS:response.server.processing.time"              , "123456000"                           ));

        fieldsTests.add(new SingleFieldTestcase("$request_uri"             , "/?aap&noot=&mies=wim"               , "HTTP.URI:request.firstline.uri"                            , "/?aap&noot=&mies=wim"                ));
        fieldsTests.add(new SingleFieldTestcase("$scheme"                  , "http"                               , "HTTP.PROTOCOL:request.firstline.uri.protocol"              , "http"                                ));
        fieldsTests.add(new SingleFieldTestcase("$sent_http_etag"          , "W/\\x22586bbb8b-29e\\x22"           , "HTTP.HEADER:response.header.etag"                          , "W/\\x22586bbb8b-29e\\x22"            ));
        fieldsTests.add(new SingleFieldTestcase("$sent_http_last_modified" , "Tue, 03 Jan 2017 14:56:11 GMT"      , "HTTP.HEADER:response.header.last_modified"                 , "Tue, 03 Jan 2017 14:56:11 GMT"       ));
        fieldsTests.add(new SingleFieldTestcase("$server_addr"             , "127.0.0.1"                          , "IP:connection.server.ip"                                   , "127.0.0.1"                           ));
        fieldsTests.add(new SingleFieldTestcase("$server_name"             , "_"                                  , "STRING:connection.server.name"                             , "_"                                   ));
        fieldsTests.add(new SingleFieldTestcase("$server_port"             , "80"                                 , "PORT:connection.server.port"                               , "80"                                  ));
        fieldsTests.add(new SingleFieldTestcase("$server_protocol"         , "HTTP/1.1"                           , "HTTP.PROTOCOL_VERSION:request.firstline.protocol"          , "HTTP/1.1"                            ));
        fieldsTests.add(new SingleFieldTestcase("$server_protocol"         , "HTTP/1.1"                           , "HTTP.PROTOCOL:request.firstline.protocol"                  , "HTTP"                            ));
        fieldsTests.add(new SingleFieldTestcase("$server_protocol"         , "HTTP/1.1"                           , "HTTP.PROTOCOL.VERSION:request.firstline.protocol.version"  , "1.1"                            ));
        fieldsTests.add(new SingleFieldTestcase("$tcpinfo_rtt"             , "52"                                 , "MICROSECONDS:connection.tcpinfo.rtt"                       , "52"                                  ));
        fieldsTests.add(new SingleFieldTestcase("$tcpinfo_rttvar"          , "30"                                 , "MICROSECONDS:connection.tcpinfo.rttvar"                    , "30"                                  ));
        fieldsTests.add(new SingleFieldTestcase("$tcpinfo_snd_cwnd"        , "10"                                 , "BYTES:connection.tcpinfo.send.cwnd"                        , "10"                                  ));
        fieldsTests.add(new SingleFieldTestcase("$tcpinfo_rcv_space"       , "43690"                              , "BYTES:connection.tcpinfo.receive.space"                    , "43690"                               ));
        fieldsTests.add(new SingleFieldTestcase("$uri"                     , "/index.html"                        , "HTTP.URI:request.firstline.uri.normalized"                 , "/index.html"                         ));
        fieldsTests.add(new SingleFieldTestcase("$document_uri"            , "/index.html"                        , "HTTP.URI:request.firstline.uri.normalized"                 , "/index.html"                         ));
        fieldsTests.add(new SingleFieldTestcase("$http_user_agent"         , "Mozilla/5.0 (Foo)"                  , "HTTP.USERAGENT:request.user-agent"                         , "Mozilla/5.0 (Foo)"                   ));
        fieldsTests.add(new SingleFieldTestcase("$http_foo_user_agent"     , "Mozilla/5.0 (Foo)"                  , "HTTP.HEADER:request.header.foo_user_agent"                 , "Mozilla/5.0 (Foo)"                   ));
        fieldsTests.add(new SingleFieldTestcase("$http_user_agent_foo"     , "Mozilla/5.0 (Foo)"                  , "HTTP.HEADER:request.header.user_agent_foo"                 , "Mozilla/5.0 (Foo)"                   ));
        fieldsTests.add(new SingleFieldTestcase("$http_referer"            , "http://localhost/"                  , "HTTP.URI:request.referer"                                  , "http://localhost/"                   ));

        // TODO: Check if these are REALLY "not intended for logging"
        fieldsTests.add(new SingleFieldTestcase("$request_body"            , "-"                                  , "NOT_IMPLEMENTED:nginx_parameter_not_intended_for_logging__request_body"        , null  ));
        fieldsTests.add(new SingleFieldTestcase("$request_body_file"       , "-"                                  , "NOT_IMPLEMENTED:nginx_parameter_not_intended_for_logging__request_body_file"   , null  ));
        fieldsTests.add(new SingleFieldTestcase("$limit_rate"              , "0"                                  , "NOT_IMPLEMENTED:nginx_parameter_not_intended_for_logging__limit_rate"          ,   "0" ));

        for (SingleFieldTestcase testCase: fieldsTests) {
            DissectorTester.create()
                .printSeparator()
                .verbose()
                .withParser(new HttpdLoglineParser<>(TestRecord.class, testCase.logformat))
                .withInput(testCase.logline)
                .expect(testCase.fieldName, testCase.expectedValue)
                .printPossible()
                .printAllPossibleValues()
                .checkExpectations()
                ;
        }

    }

    @Test
    public void validateAllFieldsPrefix() {
        List<SingleFieldTestcase> fieldsTests = new ArrayList<>();

        final String useragent = "Mozilla/5.0 (Foo)";
        fieldsTests.add(new SingleFieldTestcase("$http_user_agent"      , useragent, "HTTP.USERAGENT:request.user-agent"        , useragent));
        fieldsTests.add(new SingleFieldTestcase("$http_foo_user_agent"  , useragent, "HTTP.HEADER:request.header.foo_user_agent", useragent));
        fieldsTests.add(new SingleFieldTestcase("$http_user_agent_foo"  , useragent, "HTTP.HEADER:request.header.user_agent_foo", useragent));

        for (SingleFieldTestcase testCase: fieldsTests) {
            DissectorTester.create()
                .printSeparator()
                .verbose()
                .withParser(new HttpdLoglineParser<>(TestRecord.class, testCase.logformat))
                .withInput(testCase.logline)
                .expect(testCase.fieldName, testCase.expectedValue)
                .printPossible()
                .printAllPossibleValues()
                .checkExpectations()
            ;
        }

    }



}
