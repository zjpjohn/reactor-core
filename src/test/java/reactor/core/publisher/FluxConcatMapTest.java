/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.core.publisher;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.FluxConcatMap.ErrorMode;
import reactor.test.TestSubscriber;
import reactor.util.concurrent.QueueSupplier;

public class FluxConcatMapTest {

	@Test
	public void normal() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 2)
		    .concatMap(v -> Flux.range(v, 2))
		    .subscribe(ts);

		ts.assertValues(1, 2, 2, 3)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void normal2() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 2)
		    .hide()
		    .concatMap(v -> Flux.range(v, 2))
		    .subscribe(ts);

		ts.assertValues(1, 2, 2, 3)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void normalBoundary() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 2)
		    .concatMapDelayError(v -> Flux.range(v, 2))
		    .subscribe(ts);

		ts.assertValues(1, 2, 2, 3)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void normalBoundary2() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 2)
		    .hide()
		    .concatMapDelayError(v -> Flux.range(v, 2))
		    .subscribe(ts);

		ts.assertValues(1, 2, 2, 3)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void normalLongRun() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 1000)
		    .concatMap(v -> Flux.range(v, 1000))
		    .subscribe(ts);

		ts.assertValueCount(1_000_000)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void normalLongRunJust() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 1000_000)
		    .concatMap(v -> Flux.just(v))
		    .subscribe(ts);

		ts.assertValueCount(1_000_000)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void normalLongRun2() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 1000)
		    .hide()
		    .concatMap(v -> Flux.range(v, 1000))
		    .subscribe(ts);

		ts.assertValueCount(1_000_000)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void normalLongRunBoundary() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 1000)
		    .concatMapDelayError(v -> Flux.range(v, 1000))
		    .subscribe(ts);

		ts.assertValueCount(1_000_000)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void normalLongRunJustBoundary() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 1000_000)
		    .concatMapDelayError(v -> Flux.just(v))
		    .subscribe(ts);

		ts.assertValueCount(1_000_000)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void normalLongRunBoundary2() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 1000)
		    .hide()
		    .concatMapDelayError(v -> Flux.range(v, 1000))
		    .subscribe(ts);

		ts.assertValueCount(1_000_000)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void singleSubscriberOnly() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		DirectProcessor<Integer> source = DirectProcessor.create();

		DirectProcessor<Integer> source1 = DirectProcessor.create();
		DirectProcessor<Integer> source2 = DirectProcessor.create();

		source.concatMap(v -> v == 1 ? source1 : source2)
		      .subscribe(ts);

		ts.assertNoValues()
		  .assertNoError()
		  .assertNotComplete();

		source.onNext(1);
		source.onNext(2);

		Assert.assertTrue("source1 no subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());

		source1.onNext(1);
		source2.onNext(10);

		source1.onComplete();
		source.onComplete();

		source2.onNext(2);
		source2.onComplete();

		ts.assertValues(1, 2)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void singleSubscriberOnlyBoundary() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		DirectProcessor<Integer> source = DirectProcessor.create();

		DirectProcessor<Integer> source1 = DirectProcessor.create();
		DirectProcessor<Integer> source2 = DirectProcessor.create();

		source.concatMapDelayError(v -> v == 1 ? source1 : source2)
		      .subscribe(ts);

		ts.assertNoValues()
		  .assertNoError()
		  .assertNotComplete();

		source.onNext(1);

		Assert.assertTrue("source1 no subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());

		source1.onNext(1);
		source2.onNext(10);

		source1.onComplete();
		source.onNext(2);
		source.onComplete();

		source2.onNext(2);
		source2.onComplete();

		ts.assertValues(1, 2)
		  .assertNoError()
		  .assertComplete();

		Assert.assertFalse("source1 has subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());
	}

	@Test
	public void mainErrorsImmediate() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		DirectProcessor<Integer> source = DirectProcessor.create();

		DirectProcessor<Integer> source1 = DirectProcessor.create();
		DirectProcessor<Integer> source2 = DirectProcessor.create();

		source.concatMap(v -> v == 1 ? source1 : source2)
		      .subscribe(ts);

		ts.assertNoValues()
		  .assertNoError()
		  .assertNotComplete();

		source.onNext(1);

		Assert.assertTrue("source1 no subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());

		source1.onNext(1);

		source.onError(new RuntimeException("forced failure"));

		ts.assertValues(1)
		  .assertError(RuntimeException.class)
		  .assertErrorMessage("forced failure")
		  .assertNotComplete();

		Assert.assertFalse("source1 has subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());
	}

	@Test
	public void mainErrorsBoundary() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		DirectProcessor<Integer> source = DirectProcessor.create();

		DirectProcessor<Integer> source1 = DirectProcessor.create();
		DirectProcessor<Integer> source2 = DirectProcessor.create();

		source.concatMapDelayError(v -> v == 1 ? source1 : source2)
		      .subscribe(ts);

		ts.assertNoValues()
		  .assertNoError()
		  .assertNotComplete();

		source.onNext(1);

		Assert.assertTrue("source1 no subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());

		source1.onNext(1);

		source.onError(new RuntimeException("forced failure"));

		ts.assertValues(1)
		  .assertNoError()
		  .assertNotComplete();

		source1.onNext(2);
		source1.onComplete();

		ts.assertValues(1, 2)
		  .assertError(RuntimeException.class)
		  .assertErrorMessage("forced failure")
		  .assertNotComplete();

		Assert.assertFalse("source1 has subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());
	}

	@Test
	public void innerErrorsImmediate() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		DirectProcessor<Integer> source = DirectProcessor.create();

		DirectProcessor<Integer> source1 = DirectProcessor.create();
		DirectProcessor<Integer> source2 = DirectProcessor.create();

		source.concatMap(v -> v == 1 ? source1 : source2)
		      .subscribe(ts);

		ts.assertNoValues()
		  .assertNoError()
		  .assertNotComplete();

		source.onNext(1);

		Assert.assertTrue("source1 no subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());

		source1.onNext(1);

		source1.onError(new RuntimeException("forced failure"));

		ts.assertValues(1)
		  .assertError(RuntimeException.class)
		  .assertErrorMessage("forced failure")
		  .assertNotComplete();

		Assert.assertFalse("source1 has subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());
	}

	@Test
	public void innerErrorsBoundary() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		DirectProcessor<Integer> source = DirectProcessor.create();

		DirectProcessor<Integer> source1 = DirectProcessor.create();
		DirectProcessor<Integer> source2 = DirectProcessor.create();

		source.concatMapDelayError(v -> v == 1 ? source1 : source2)
		      .subscribe(ts);

		ts.assertNoValues()
		  .assertNoError()
		  .assertNotComplete();

		source.onNext(1);

		Assert.assertTrue("source1 no subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());

		source1.onNext(1);

		source1.onError(new RuntimeException("forced failure"));

		ts.assertValues(1)
		  .assertError(RuntimeException.class)
		  .assertErrorMessage("forced failure")
		  .assertNotComplete();

		Assert.assertFalse("source1 has subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());
	}

	@Test
	public void innerErrorsEnd() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		DirectProcessor<Integer> source = DirectProcessor.create();

		DirectProcessor<Integer> source1 = DirectProcessor.create();
		DirectProcessor<Integer> source2 = DirectProcessor.create();

		source.concatMapDelayError(v -> v == 1 ? source1 : source2, true, 32)
		      .subscribe(ts);

		ts.assertNoValues()
		  .assertNoError()
		  .assertNotComplete();

		source.onNext(1);

		Assert.assertTrue("source1 no subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());

		source1.onNext(1);

		source1.onError(new RuntimeException("forced failure"));

		source.onNext(2);

		Assert.assertTrue("source2 no subscribers?", source2.hasDownstreams());

		source2.onNext(2);
		source2.onComplete();

		source.onComplete();

		ts.assertValues(1, 2)
		  .assertError(RuntimeException.class)
		  .assertErrorMessage("forced failure")
		  .assertNotComplete();

		Assert.assertFalse("source1 has subscribers?", source1.hasDownstreams());
		Assert.assertFalse("source2 has subscribers?", source2.hasDownstreams());
	}

	@Test
	public void syncFusionMapToNull() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 2)
		    .map(v -> v == 2 ? null : v)
		    .concatMap(Flux::just)
		    .subscribe(ts);

		ts.assertValues(1)
		  .assertError(NullPointerException.class)
		  .assertNotComplete();
	}

	@Test
	public void syncFusionMapToNullFilter() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 2)
		    .map(v -> v == 2 ? null : v)
		    .filter(v -> true)
		    .concatMap(Flux::just)
		    .subscribe(ts);

		ts.assertValues(1)
		  .assertError(NullPointerException.class)
		  .assertNotComplete();
	}

	@Test
	public void asyncFusionMapToNull() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		UnicastProcessor<Integer> up = UnicastProcessor.create(QueueSupplier.<Integer>get(2).get());
		up.onNext(1);
		up.onNext(2);
		up.onComplete();

		up.map(v -> v == 2 ? null : v)
		  .concatMap(Flux::just)
		  .subscribe(ts);

		ts.assertValues(1)
		  .assertError(NullPointerException.class)
		  .assertNotComplete();
	}

	@Test
	public void asyncFusionMapToNullFilter() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		UnicastProcessor<Integer> up =
				UnicastProcessor.create(QueueSupplier.<Integer>get(2).get());
		up.onNext(1);
		up.onNext(2);
		up.onComplete();

		up.map(v -> v == 2 ? null : v)
		  .filter(v -> true)
		  .concatMap(Flux::just)
		  .subscribe(ts);

		ts.assertValues(1)
		  .assertError(NullPointerException.class)
		  .assertNotComplete();
	}

	@Test
	public void scalarAndRangeBackpressured() {
		TestSubscriber<Integer> ts = TestSubscriber.create(0);

		@SuppressWarnings("unchecked") Publisher<Integer>[] sources =
				new Publisher[]{Flux.just(1), Flux.range(2, 3)};

		Flux.range(0, 2)
		    .concatMap(v -> sources[v])
		    .subscribe(ts);

		ts.assertNoValues()
		  .assertNoError();

		ts.request(5);

		ts.assertValues(1, 2, 3, 4)
		  .assertComplete()
		  .assertNoError();
	}

	@Test
	public void allEmptyBackpressured() {
		TestSubscriber<Integer> ts = TestSubscriber.create(0);

		Flux.range(0, 10)
		    .hide()
		    .concatMap(v -> Flux.<Integer>empty(), 2)
		    .subscribe(ts);

		ts.assertNoValues()
		  .assertComplete()
		  .assertNoError();
	}

}
