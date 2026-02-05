package com.lioncorp.sonar.mulesoft.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPatternsTest {

    @Test
    void testIsDangerousClass() {
        // Dangerous classes
        assertThat(SecurityPatterns.isDangerousClass("java.lang.Runtime")).isTrue();
        assertThat(SecurityPatterns.isDangerousClass("java.lang.ProcessBuilder")).isTrue();
        assertThat(SecurityPatterns.isDangerousClass("java.io.File")).isTrue();
        assertThat(SecurityPatterns.isDangerousClass("sun.misc.Unsafe")).isTrue();
        assertThat(SecurityPatterns.isDangerousClass("com.sun.Something")).isTrue();

        // Safe classes
        assertThat(SecurityPatterns.isDangerousClass("java.lang.String")).isFalse();
        assertThat(SecurityPatterns.isDangerousClass("java.util.ArrayList")).isFalse();
        assertThat(SecurityPatterns.isDangerousClass(null)).isFalse();
    }

    @Test
    void testIsDangerousMethod() {
        assertThat(SecurityPatterns.isDangerousMethod("exec")).isTrue();
        assertThat(SecurityPatterns.isDangerousMethod("eval")).isTrue();
        assertThat(SecurityPatterns.isDangerousMethod("invoke")).isTrue();
        assertThat(SecurityPatterns.isDangerousMethod("loadClass")).isTrue();
        assertThat(SecurityPatterns.isDangerousMethod("forName")).isTrue();

        assertThat(SecurityPatterns.isDangerousMethod("toString")).isFalse();
        assertThat(SecurityPatterns.isDangerousMethod(null)).isFalse();
    }

    @Test
    void testIsFileIOClass() {
        assertThat(SecurityPatterns.isFileIOClass("java.io.File")).isTrue();
        assertThat(SecurityPatterns.isFileIOClass("java.io.FileReader")).isTrue();
        assertThat(SecurityPatterns.isFileIOClass("java.nio.file.Files")).isTrue();

        assertThat(SecurityPatterns.isFileIOClass("java.lang.String")).isFalse();
        assertThat(SecurityPatterns.isFileIOClass(null)).isFalse();
    }

    @Test
    void testIsNetworkClass() {
        assertThat(SecurityPatterns.isNetworkClass("java.net.Socket")).isTrue();
        assertThat(SecurityPatterns.isNetworkClass("java.net.URL")).isTrue();
        assertThat(SecurityPatterns.isNetworkClass("java.net.HttpURLConnection")).isTrue();

        assertThat(SecurityPatterns.isNetworkClass("java.lang.String")).isFalse();
        assertThat(SecurityPatterns.isNetworkClass(null)).isFalse();
    }

    @Test
    void testIsScriptEngineClass() {
        assertThat(SecurityPatterns.isScriptEngineClass("javax.script.ScriptEngine")).isTrue();
        assertThat(SecurityPatterns.isScriptEngineClass("groovy.lang.GroovyShell")).isTrue();

        assertThat(SecurityPatterns.isScriptEngineClass("java.lang.String")).isFalse();
        assertThat(SecurityPatterns.isScriptEngineClass(null)).isFalse();
    }

    @Test
    void testIsReflectionClass() {
        assertThat(SecurityPatterns.isReflectionClass("java.lang.Class")).isTrue();
        assertThat(SecurityPatterns.isReflectionClass("java.lang.reflect.Method")).isTrue();

        assertThat(SecurityPatterns.isReflectionClass("java.lang.String")).isFalse();
        assertThat(SecurityPatterns.isReflectionClass(null)).isFalse();
    }

    @Test
    void testIsWeakCryptoAlgorithm() {
        assertThat(SecurityPatterns.isWeakCryptoAlgorithm("DES")).isTrue();
        assertThat(SecurityPatterns.isWeakCryptoAlgorithm("MD5")).isTrue();
        assertThat(SecurityPatterns.isWeakCryptoAlgorithm("SHA1")).isTrue();
        assertThat(SecurityPatterns.isWeakCryptoAlgorithm("RC4")).isTrue();
        assertThat(SecurityPatterns.isWeakCryptoAlgorithm("des")).isTrue();
        assertThat(SecurityPatterns.isWeakCryptoAlgorithm("md5")).isTrue();

        assertThat(SecurityPatterns.isWeakCryptoAlgorithm("AES")).isFalse();
        assertThat(SecurityPatterns.isWeakCryptoAlgorithm("SHA256")).isFalse();
        assertThat(SecurityPatterns.isWeakCryptoAlgorithm(null)).isFalse();
    }

    @Test
    void testIsStrongCryptoAlgorithm() {
        assertThat(SecurityPatterns.isStrongCryptoAlgorithm("AES")).isTrue();
        assertThat(SecurityPatterns.isStrongCryptoAlgorithm("AES-256")).isTrue();
        assertThat(SecurityPatterns.isStrongCryptoAlgorithm("SHA-256")).isTrue();
        assertThat(SecurityPatterns.isStrongCryptoAlgorithm("RSA")).isTrue();
        assertThat(SecurityPatterns.isStrongCryptoAlgorithm("aes-256")).isTrue();

        assertThat(SecurityPatterns.isStrongCryptoAlgorithm("DES")).isFalse();
        assertThat(SecurityPatterns.isStrongCryptoAlgorithm("MD5")).isFalse();
        assertThat(SecurityPatterns.isStrongCryptoAlgorithm(null)).isFalse();
    }

    @Test
    void testIsCleartextProtocol() {
        assertThat(SecurityPatterns.isCleartextProtocol("HTTP")).isTrue();
        assertThat(SecurityPatterns.isCleartextProtocol("FTP")).isTrue();
        assertThat(SecurityPatterns.isCleartextProtocol("SMTP")).isTrue();
        assertThat(SecurityPatterns.isCleartextProtocol("http")).isTrue();

        assertThat(SecurityPatterns.isCleartextProtocol("HTTPS")).isFalse();
        assertThat(SecurityPatterns.isCleartextProtocol("FTPS")).isFalse();
        assertThat(SecurityPatterns.isCleartextProtocol(null)).isFalse();
    }

    @Test
    void testIsSecureProtocol() {
        assertThat(SecurityPatterns.isSecureProtocol("HTTPS")).isTrue();
        assertThat(SecurityPatterns.isSecureProtocol("FTPS")).isTrue();
        assertThat(SecurityPatterns.isSecureProtocol("SFTP")).isTrue();
        assertThat(SecurityPatterns.isSecureProtocol("TLS")).isTrue();
        assertThat(SecurityPatterns.isSecureProtocol("https")).isTrue();

        assertThat(SecurityPatterns.isSecureProtocol("HTTP")).isFalse();
        assertThat(SecurityPatterns.isSecureProtocol("FTP")).isFalse();
        assertThat(SecurityPatterns.isSecureProtocol(null)).isFalse();
    }

    @Test
    void testHasSQLInjectionRisk() {
        assertThat(SecurityPatterns.hasSQLInjectionRisk("SELECT * FROM users WHERE id = \" + userId")).isTrue();
        assertThat(SecurityPatterns.hasSQLInjectionRisk("INSERT INTO table VALUES (\" + value + \")")).isTrue();
        assertThat(SecurityPatterns.hasSQLInjectionRisk("UPDATE users SET name = \"$ + name")).isTrue();
        assertThat(SecurityPatterns.hasSQLInjectionRisk("DELETE FROM users WHERE id = '$ + id")).isTrue();

        assertThat(SecurityPatterns.hasSQLInjectionRisk("SELECT * FROM users WHERE id = ?")).isFalse();
        assertThat(SecurityPatterns.hasSQLInjectionRisk("Some random text")).isFalse();
        assertThat(SecurityPatterns.hasSQLInjectionRisk(null)).isFalse();
    }

    @Test
    void testHasCommandInjectionRisk() {
        assertThat(SecurityPatterns.hasCommandInjectionRisk("Runtime.getRuntime().exec(cmd)")).isTrue();
        assertThat(SecurityPatterns.hasCommandInjectionRisk("new ProcessBuilder(command)")).isTrue();
        assertThat(SecurityPatterns.hasCommandInjectionRisk("bash -c \" + input")).isTrue();
        assertThat(SecurityPatterns.hasCommandInjectionRisk("cmd.exe /c dir")).isTrue();

        assertThat(SecurityPatterns.hasCommandInjectionRisk("System.out.println()")).isFalse();
        assertThat(SecurityPatterns.hasCommandInjectionRisk(null)).isFalse();
    }

    @Test
    void testHasPathTraversalRisk() {
        assertThat(SecurityPatterns.hasPathTraversalRisk("../../../etc/passwd")).isTrue();
        assertThat(SecurityPatterns.hasPathTraversalRisk("..\\..\\windows\\system32")).isTrue();
        assertThat(SecurityPatterns.hasPathTraversalRisk("%2e%2e/file")).isTrue();
        assertThat(SecurityPatterns.hasPathTraversalRisk("%252e%252e/file")).isTrue();

        assertThat(SecurityPatterns.hasPathTraversalRisk("/home/user/file")).isFalse();
        assertThat(SecurityPatterns.hasPathTraversalRisk("file.txt")).isFalse();
        assertThat(SecurityPatterns.hasPathTraversalRisk(null)).isFalse();
    }

    @Test
    void testHasXXERisk() {
        assertThat(SecurityPatterns.hasXXERisk("DocumentBuilder builder = factory.newDocumentBuilder()")).isTrue();
        assertThat(SecurityPatterns.hasXXERisk("SAXParser parser = factory.newSAXParser()")).isTrue();
        assertThat(SecurityPatterns.hasXXERisk("XMLReader reader = XMLReaderFactory.createXMLReader()")).isTrue();

        assertThat(SecurityPatterns.hasXXERisk("DocumentBuilder with DISALLOW_DOCTYPE_DECL")).isFalse();
        assertThat(SecurityPatterns.hasXXERisk("parser with external-general-entities false")).isFalse();
        assertThat(SecurityPatterns.hasXXERisk("String text = \"test\"")).isFalse();
        assertThat(SecurityPatterns.hasXXERisk(null)).isFalse();
    }

    @Test
    void testHasDeserializationRisk() {
        assertThat(SecurityPatterns.hasDeserializationRisk("ObjectInputStream ois = new ObjectInputStream(input); ois.readObject()")).isTrue();
        // Pattern requires full context with ObjectInputStream, not just readObject()
        assertThat(SecurityPatterns.hasDeserializationRisk("stream.readObject()")).isFalse();

        assertThat(SecurityPatterns.hasDeserializationRisk("BufferedReader reader")).isFalse();
        assertThat(SecurityPatterns.hasDeserializationRisk(null)).isFalse();
    }

    @Test
    void testUsesInsecureRandom() {
        assertThat(SecurityPatterns.usesInsecureRandom("double r = Math.random()")).isTrue();
        assertThat(SecurityPatterns.usesInsecureRandom("Random rnd = new Random()")).isTrue();

        assertThat(SecurityPatterns.usesInsecureRandom("SecureRandom rnd = new SecureRandom()")).isFalse();
        assertThat(SecurityPatterns.usesInsecureRandom("new Random() with SecureRandom")).isFalse();
        assertThat(SecurityPatterns.usesInsecureRandom(null)).isFalse();
    }

    @Test
    void testHasInsecureCORS() {
        assertThat(SecurityPatterns.hasInsecureCORS("*")).isTrue();

        assertThat(SecurityPatterns.hasInsecureCORS("https://example.com")).isFalse();
        assertThat(SecurityPatterns.hasInsecureCORS("https://trusted.domain")).isFalse();
        assertThat(SecurityPatterns.hasInsecureCORS(null)).isFalse();
    }

    @Test
    void testHasUnvalidatedRedirect() {
        assertThat(SecurityPatterns.hasUnvalidatedRedirect("response.redirect(request.getParameter(\"url\"))")).isTrue();
        assertThat(SecurityPatterns.hasUnvalidatedRedirect("forward to payload.url")).isTrue();
        assertThat(SecurityPatterns.hasUnvalidatedRedirect("redirect to vars.targetUrl")).isTrue();

        assertThat(SecurityPatterns.hasUnvalidatedRedirect("redirect to https://example.com")).isFalse();
        assertThat(SecurityPatterns.hasUnvalidatedRedirect(null)).isFalse();
    }

    @Test
    void testExposesStackTrace() {
        assertThat(SecurityPatterns.exposesStackTrace("e.printStackTrace()")).isTrue();
        assertThat(SecurityPatterns.exposesStackTrace("exception.getMessage()")).isTrue();
        assertThat(SecurityPatterns.exposesStackTrace("String msg = e.toString()")).isTrue();

        assertThat(SecurityPatterns.exposesStackTrace("logger.error(\"Error occurred\")")).isFalse();
        assertThat(SecurityPatterns.exposesStackTrace(null)).isFalse();
    }

    @Test
    void testSwallowsException() {
        assertThat(SecurityPatterns.swallowsException("catch (Exception e) {}")).isTrue();
        assertThat(SecurityPatterns.swallowsException("try { } catch { }")).isTrue();

        assertThat(SecurityPatterns.swallowsException("catch (Exception e) { log.error(e); }")).isFalse();
        assertThat(SecurityPatterns.swallowsException("catch (Exception e) { throw new RuntimeException(e); }")).isFalse();
        assertThat(SecurityPatterns.swallowsException(null)).isFalse();
    }

    @Test
    void testGetDangerousClassReason() {
        assertThat(SecurityPatterns.getDangerousClassReason("java.io.File")).contains("File I/O");
        assertThat(SecurityPatterns.getDangerousClassReason("java.net.Socket")).contains("Network");
        assertThat(SecurityPatterns.getDangerousClassReason("javax.script.ScriptEngine")).contains("Script");
        assertThat(SecurityPatterns.getDangerousClassReason("java.lang.Class")).contains("Reflection");
        assertThat(SecurityPatterns.getDangerousClassReason("java.lang.Runtime")).contains("system commands");
        assertThat(SecurityPatterns.getDangerousClassReason(null)).isEqualTo("Unknown class");
    }

    @Test
    void testEdgeCasesWithEmptyStrings() {
        assertThat(SecurityPatterns.isDangerousClass("")).isFalse();
        assertThat(SecurityPatterns.isDangerousMethod("")).isFalse();
        assertThat(SecurityPatterns.isWeakCryptoAlgorithm("")).isFalse();
        assertThat(SecurityPatterns.hasSQLInjectionRisk("")).isFalse();
    }

    @Test
    void testCaseInsensitivity() {
        assertThat(SecurityPatterns.isWeakCryptoAlgorithm("des")).isTrue();
        assertThat(SecurityPatterns.isWeakCryptoAlgorithm("DES")).isTrue();
        assertThat(SecurityPatterns.isWeakCryptoAlgorithm("Des")).isTrue();

        assertThat(SecurityPatterns.isCleartextProtocol("http")).isTrue();
        assertThat(SecurityPatterns.isCleartextProtocol("HTTP")).isTrue();
        assertThat(SecurityPatterns.isCleartextProtocol("Http")).isTrue();
    }
}
