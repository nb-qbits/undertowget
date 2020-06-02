/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.uscellular.service.consumer.binding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.undertow.DefaultUndertowHttpBinding;
import org.apache.camel.component.undertow.UndertowHeaderFilterStrategy;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.BlockingReadableByteChannel;
import org.xnio.channels.StreamSourceChannel;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Methods;

/**
 * DefaultUndertowHttpBinding represent binding used by default, if user doesn't
 * provide any. By default {@link UndertowHeaderFilterStrategy} is also used.
 * https://github.com/apache/camel/blob/camel-2.25.x/components/camel-undertow/src/main/java/org/apache/camel/component/undertow/DefaultUndertowHttpBinding.java
 */
public class CustomUndertowHttpBinding extends DefaultUndertowHttpBinding {

	private static final Logger LOG = LoggerFactory.getLogger(CustomUndertowHttpBinding.class);

	// use default filter strategy from Camel HTTP
	private HeaderFilterStrategy headerFilterStrategy;
	private static DefaultUndertowHttpBinding binder;

	@Override
	public Message toCamelMessage(HttpServerExchange httpExchange, Exchange exchange) throws Exception {
		Message result = new DefaultMessage(exchange.getContext());

		populateCamelHeaders(httpExchange, result.getHeaders(), exchange);

		// Map form data which is parsed by undertow form parsers
		FormData formData = httpExchange.getAttachment(FormDataParser.FORM_DATA);
		if (formData != null) {
			Message message = binder.toCamelMessage(httpExchange, exchange);
			result.setBody(message.getBody());
		} else {
			// extract body by myself if undertow parser didn't handle and the method is
			// allowed to have one
			// body is extracted as byte[] then auto TypeConverter kicks in
			if (Methods.GET.equals(httpExchange.getRequestMethod())
					|| Methods.POST.equals(httpExchange.getRequestMethod())
					|| Methods.PUT.equals(httpExchange.getRequestMethod())
					|| Methods.PATCH.equals(httpExchange.getRequestMethod())) {
				result.setBody(readFromThisChannel(httpExchange.getRequestChannel()));
			} else {
				result.setBody(null);
			}
		}
		return result;
	}

	byte[] readFromThisChannel(StreamSourceChannel source) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ByteBuffer buffer = ByteBuffer.wrap(new byte[1024]);

		@SuppressWarnings("resource")
		ReadableByteChannel blockingSource = new BlockingReadableByteChannel(source);

		for (;;) {
			int res = blockingSource.read(buffer);
			if (res == -1) {
				return out.toByteArray();
			} else if (res == 0) {
				LOG.error("Channel did not block");
			} else {
				buffer.flip();
				out.write(buffer.array(), buffer.arrayOffset() + buffer.position(),
						buffer.arrayOffset() + buffer.limit());
				buffer.clear();
			}
		}
	}
}