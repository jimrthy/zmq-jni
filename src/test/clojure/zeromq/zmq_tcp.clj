;;
;; Copyright 2013-2014 Trevor Bernard
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns zeromq.zmq-tcp
  (:require [zeromq.zmq :as zmq])
  (:use clojure.test)
  (:import java.nio.ByteBuffer
           java.nio.CharBuffer))

(defn send-str
  ([socket ^String data]
     (zmq/send socket (.getBytes data)))
  ([socket ^String data flags]
     (zmq/send socket (.getBytes data) (int flags))))

(defn receive-str [socket]
  (let [data (zmq/receive socket)]
    (when data
      (String. data))))

(deftest push-pull-test
  (with-open [context (zmq/context)
              push (doto (zmq/socket context :push)
                     (zmq/connect "tcp://localhost:6001"))
              pull (doto (zmq/socket context :pull)
                     (zmq/bind "tcp://*:6001"))]
    (send-str push "helloworld")
    (let [actual (receive-str pull)]
      (is (= "helloworld" actual)))))

(deftest send-bb-test
  (with-open [context (zmq/context)
              push (doto (zmq/socket context :push)
                     (zmq/connect "tcp://localhost:6001"))
              pull (doto (zmq/socket context :pull)
                     (zmq/bind "tcp://*:6001"))]
    (let [bb (doto (ByteBuffer/allocateDirect 10)
               (.put (.getBytes "helloworld"))
               (.flip))]
      (zmq/send-bb push bb))
    (let [actual (receive-str pull)]
      (is (= "helloworld" actual)))))

(deftest receive-bb-test
  (with-open [context (zmq/context)
              push (doto (zmq/socket context :push)
                     (zmq/connect "tcp://localhost:6001"))
              pull (doto (zmq/socket context :pull)
                     (zmq/bind "tcp://*:6001"))]
    (send-str push "helloworld")
    (let [bb (ByteBuffer/allocateDirect 10)
          _ (zmq/receive-bb pull bb)
          buf (byte-array 10)]
      (.flip bb)
      (.get bb buf)
      (is (= "helloworld" (String. buf))))))

(deftest receive-bb-positive-position-test
  (with-open [context (zmq/context)
              push (doto (zmq/socket context :push)
                     (zmq/connect "tcp://localhost:6001"))
              pull (doto (zmq/socket context :pull)
                     (zmq/bind "tcp://*:6001"))]
    (send-str push "helloworld")
    (let [bb (ByteBuffer/allocateDirect 12)
          _ (.position bb 2)
          _ (zmq/receive-bb pull bb)
          buf (byte-array 10)]
      (.flip bb)
      (.position bb 2)
      (.get bb buf)
      (is (= "helloworld" (String. buf))))))

(deftest receive-bb-positive-position-truncate-test
  (with-open [context (zmq/context)
              push (doto (zmq/socket context :push)
                     (zmq/connect "tcp://localhost:6001"))
              pull (doto (zmq/socket context :pull)
                     (zmq/bind "tcp://*:6001"))]
    (send-str push "helloworld")
    (let [bb (ByteBuffer/allocateDirect 7)
          _ (.position bb 2)
          _ (zmq/receive-bb pull bb)
          buf (byte-array 5)]
      (.flip bb)
      (.position bb 2)
      (.get bb buf)
      (is (= "hello" (String. buf))))))

(deftest receive-bb-truncate-test
  (with-open [context (zmq/context)
              push (doto (zmq/socket context :push)
                     (zmq/connect "tcp://localhost:6001"))
              pull (doto (zmq/socket context :pull)
                     (zmq/bind "tcp://*:6001"))]
    (send-str push "helloworld")
    (let [bb (ByteBuffer/allocateDirect 5)
          size (zmq/receive-bb pull bb)
          buf (byte-array size)]
      (.flip bb)
      (.get bb buf)
      (is (= "hello" (String. buf))))))

(deftest pub-sub-test
  (with-open [context (zmq/context)
              sub (doto (zmq/socket context :sub)
                     (zmq/connect "tcp://localhost:6001")
                     (zmq/subscribe (.getBytes "A")))
              pub (doto (zmq/socket context :pub)
                     (zmq/bind "tcp://*:6001"))]
    (Thread/sleep 200)
    (send-str pub "A" zmq/send-more)
    (send-str pub "helloworld")
    (zmq/receive sub 0) ;; eat topic
    (let [actual (receive-str sub)]
      (is (= "helloworld" actual)))))

(deftest multi-part-test
  (with-open [context (zmq/context)
              push (doto (zmq/socket context :push)
                     (zmq/connect "tcp://localhost:6001"))
              pull (doto (zmq/socket context :pull)
                     (zmq/bind "tcp://*:6001"))]
    (send-str push "hello" zmq/send-more)
    (send-str push "world")
    (zmq/receive pull 0)
    (is (zmq/receive-more? pull))
    (zmq/receive pull 0)
    (is (not (zmq/receive-more? pull)))))

(deftest curve-keypair-basics
  ;; Q: Should these be 40 or 41?
  (let [public (CharBuffer/allocate 41)
        secret (CharBuffer/allocate 41)]
    (is (zmq/curve-keypair public secret) "Key Pair Generation Failed")
    (is (not= (into [] public) (into [] secret)))))

(deftest z85-encoding-test
  (let [raw-bytes (byte-array (map unchecked-byte [0x86 0x4F 0xD2 0x6F 0xB5 0x59 0xF7 0x5B]))
        char-buffer (CharBuffer/allocate 10)]
    (zmq/z85-encode char-buffer raw-bytes)
    (is (= "HelloWorld") (str char-buffer))))

(deftest z85-decoding-test
  (let [expected (byte-array (map unchecked-byte [0x86 0x4F 0xD2 0x6F 0xB5 0x59 0xF7 0x5B]))
        dest (byte-array 8)]
    (zmq/z85-dencode dest "HelloWorld")
    (println "Expected:" (apply str (map #(format "%02X " %) expected)))
    (println "Decoded :" (apply str (map #(format "%02X " %) dest)))
    (is (java.util.Arrays/equals expected dest))))

(deftest exception-throwing-recv-test
  (with-open [context (zmq/context)
              pull (doto (zmq/socket context :pull)
                     (zmq/connect "tcp://localhost:6001"))]
    (testing "Fail to receive and return an array"
      (try
        (zmq/receive-safe pull zmq/dont-wait)
        (is false "Should have thrown an exception")
        (catch Exception ex
          ;; Some sort of exception belongs here.
          ;; Q: Which kind actually makes sense?
          )))
    (testing "Fail to receive into byte array"
      (try
        (let [buffer (byte-array (range 40))]
          (zmq/receive-safe pull buffer zmq/dont-wait)
          (is false "Should have thrown an exception"))
        (catch RuntimeException ex
          ;; This exception type's for errors in the JVM code.
          ;; Which, really, this probably is.
          ;; Or maybe it's a problem with the other side of the socket.
          ;; Or maybe it's a broken network cable.
          ;; Without checking errno, there's no way to tell.
          ;; Of course, we could always check errno here.
          ;; Seems like it would be better to throw meaningful
          ;; exceptions in the first place, but that would
          ;; be an incremental improvement over the basic idea.
          )))
    (testing "Fail to receive into a ByteBuffer"
      (let [bb (ByteBuffer/allocateDirect 12)]
        (.position bb 2)
        (try
          (zmq/receive-safe-bb pull bb zmq/dont-wait)
          (is false "Should have thrown")
          (catch Exception ex))))))
