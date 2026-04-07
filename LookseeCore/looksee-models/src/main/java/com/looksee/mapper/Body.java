/*
 * Copyright 2019 Google LLC
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

package com.looksee.mapper;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body is the payload of a Pub/Sub event. Please refer to the docs for
 * additional information regarding Pub/Sub events.
 */
@NoArgsConstructor
@Getter
@Setter
public class Body {

  private Message message;

  /**
   * A message from Pub/Sub
   */
  @Getter
  @Setter
  @NoArgsConstructor
  public class Message {

    private String messageId;
    private String publishTime;
    private String data;

    /**
     * Pub/Sub message attributes. Used as the carrier for W3C trace-context
     * headers ({@code traceparent}, {@code tracestate}) so that distributed
     * traces can span the entire audit pipeline. Defaults to an empty map so
     * downstream code can call {@link Map#get(Object)} without null checks.
     */
    private Map<String, String> attributes = new HashMap<>();

    /**
     * Creates a new message
     *
     * @param messageId the message id
     * @param publishTime the publish time
     * @param data the data
     *
     * precondition: messageId != null
     * precondition: publishTime != null
     * precondition: data != null
     */
    public Message(String messageId, String publishTime, String data) {
      assert messageId != null : "messageId must not be null";
      assert publishTime != null : "publishTime must not be null";
      assert data != null : "data must not be null";

      this.messageId = messageId;
      this.publishTime = publishTime;
      this.data = data;
    }
  }
}