package dcl.com;

import sun.misc.VM;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MyClassLoader extends ClassLoader {
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String url = "file:/tmp/Tools.class";
        URL myUrl = null;
        try {
            myUrl = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLConnection connection = null;
        try {
            connection = myUrl.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStream input = null;
        try {
            input = connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int data = 0;
        try {
            data = input.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(data != -1){
            buffer.write(data);
            try {
                data = input.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] classData = buffer.toByteArray();
        return defineClass(name, classData, 0, classData.length);
    }

    protected Class<?> defineClazz(String name, byte[] b, int off, int len, ProtectionDomain protectionDomain)
            throws ClassFormatError {
        protectionDomain = preDefineClass(name, protectionDomain);
        String source = defineClassSourceLocation(protectionDomain);
        Class<?> c = defineClass1(name, b, off, len, protectionDomain, source);
        postDefineClass(c, protectionDomain);
        return c;
    }

    private native Class<?> defineClass1(String name, byte[] b, int off, int len, ProtectionDomain pd, String source);

    private String defineClassSourceLocation(ProtectionDomain pd) {
        CodeSource cs = pd.getCodeSource();
        String source = null;
        if (cs != null && cs.getLocation() != null) {
            source = cs.getLocation().toString();
        }
        return source;
    }

    private void postDefineClass(Class<?> c, ProtectionDomain pd) {
        if (pd.getCodeSource() != null) {
            Certificate certs[] = pd.getCodeSource().getCertificates();
            if (certs != null)
                setSigners(c, certs);
        }
    }

    private ProtectionDomain preDefineClass(String name, ProtectionDomain pd) {
        if (!checkName(name))
            throw new NoClassDefFoundError("IllegalName: " + name);

        if ((name != null) && name.startsWith("java.")) {
            throw new SecurityException
                    ("Prohibited package name: " +
                            name.substring(0, name.lastIndexOf('.')));
        }
        if (pd == null) {
            pd = defaultDomain;
        }

        if (name != null) checkCerts(name, pd.getCodeSource());

        return pd;
    }

    private void checkCerts(String name, CodeSource cs) {
        int i = name.lastIndexOf('.');
        String pname = (i == -1) ? "" : name.substring(0, i);

        Certificate[] certs = null;
        if (cs != null) {
            certs = cs.getCertificates();
        }
        Certificate[] pcerts = null;
        if (parallelLockMap == null) {
            synchronized (this) {
                pcerts = package2certs.get(pname);
                if (pcerts == null) {
                    package2certs.put(pname, (certs == null ? nocerts : certs));
                }
            }
        } else {
            pcerts = ((ConcurrentHashMap<String, Certificate[]>) package2certs).
                    putIfAbsent(pname, (certs == null ? nocerts : certs));
        }
        if (pcerts != null && !compareCerts(pcerts, certs)) {
            throw new SecurityException("class \"" + name +
                    "\"'s signer information does not match signer information of other classes in the same package");
        }
    }

    private boolean compareCerts(Certificate[] pcerts, Certificate[] certs) {
        // certs can be null, indicating no certs.
        if ((certs == null) || (certs.length == 0)) {
            return pcerts.length == 0;
        }

        // the length must be the same at this point
        if (certs.length != pcerts.length)
            return false;

        // go through and make sure all the certs in one array
        // are in the other and vice-versa.
        boolean match;
        for (int i = 0; i < certs.length; i++) {
            match = false;
            for (int j = 0; j < pcerts.length; j++) {
                if (certs[i].equals(pcerts[j])) {
                    match = true;
                    break;
                }
            }
            if (!match) return false;
        }

        // now do the same for pcerts
        for (int i = 0; i < pcerts.length; i++) {
            match = false;
            for (int j = 0; j < certs.length; j++) {
                if (pcerts[i].equals(certs[j])) {
                    match = true;
                    break;
                }
            }
            if (!match) return false;
        }

        return true;
    }

    private final ProtectionDomain defaultDomain =
            new ProtectionDomain(new CodeSource(null, (Certificate[]) null),
                    null, this, null);

    private static final Certificate[] nocerts = new Certificate[0];

    private ConcurrentHashMap<String, Object> parallelLockMap;

    private Map<String, Certificate[]> package2certs;

    private boolean checkName(String name) {
        return (name == null) || (name.length() == 0) || !((name.indexOf('/') != -1) || (!VM.allowArraySyntax() && (name.charAt(0) == '[')));
    }
}
