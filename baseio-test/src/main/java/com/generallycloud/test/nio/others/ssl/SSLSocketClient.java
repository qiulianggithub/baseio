/*
 * Copyright 2015 GenerallyCloud.com
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package com.generallycloud.test.nio.others.ssl;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.generallycloud.nio.common.CloseUtil;

public class SSLSocketClient {

	public static void main(String [] args) throws Exception {

		X509TrustManager x509m = new X509TrustManager() {

			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
					throws java.security.cert.CertificateException {

			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
					throws java.security.cert.CertificateException {

			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};
		SSLContext context = SSLContext.getInstance("SSL");
		// 初始化
		context.init(null, new TrustManager[] { x509m }, new SecureRandom());
		SSLSocketFactory factory = context.getSocketFactory();
		SSLSocket s = (SSLSocket) factory.createSocket("localhost", 443);
		System.out.println("ok");

		OutputStream output = s.getOutputStream();
		InputStream input = s.getInputStream();

		output.write("alert".getBytes());
		System.out.println("sent: alert");
		output.flush();

		byte[] buf = new byte[1024];
		int len = input.read(buf);
		
		CloseUtil.close(output);
		CloseUtil.close(input);
		System.out.println("received:" + new String(buf, 0, len));
	}
}
