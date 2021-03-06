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

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Test;
import reactor.test.TestSubscriber;

public class FluxFilterTest {

	@Test(expected = NullPointerException.class)
	public void sourceNull() {
		new FluxFilter<Integer>(null, e -> true);
	}

	@Test(expected = NullPointerException.class)
	public void predicateNull() {
		Flux.never()
		    .filter(null);
	}

	@Test
	public void normal() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 10)
		    .filter(v -> v % 2 == 0)
		    .subscribe(ts);

		ts.assertValues(2, 4, 6, 8, 10)
		  .assertComplete()
		  .assertNoError();
	}

	@Test
	public void normalBackpressuredRange() {
		TestSubscriber<Integer> ts = TestSubscriber.create(2);

		Flux.range(1, 10)
		    .filter(v -> v % 2 == 0)
		    .subscribe(ts);

		ts.assertValues(2, 4)
		  .assertNotComplete()
		  .assertNoError();

		ts.request(10);

		ts.assertValues(2, 4, 6, 8, 10)
		  .assertComplete()
		  .assertNoError();
	}

	@Test
	public void normalBackpressuredArray() {
		TestSubscriber<Integer> ts = TestSubscriber.create(2);

		Flux.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
		    .filter(v -> v % 2 == 0)
		    .subscribe(ts);

		ts.assertValues(2, 4)
		  .assertNotComplete()
		  .assertNoError();

		ts.request(10);

		ts.assertValues(2, 4, 6, 8, 10)
		  .assertComplete()
		  .assertNoError();
	}

	@Test
	public void normalBackpressuredIterable() {
		TestSubscriber<Integer> ts = TestSubscriber.create(2);

		Flux.fromIterable(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
		    .filter(v -> v % 2 == 0)
		    .subscribe(ts);

		ts.assertValues(2, 4)
		  .assertNotComplete()
		  .assertNoError();

		ts.request(10);

		ts.assertValues(2, 4, 6, 8, 10)
		  .assertComplete()
		  .assertNoError();
	}

	@Test
	public void predicateThrows() {
		TestSubscriber<Integer> ts = TestSubscriber.create(2);

		Flux.range(1, 10)
		    .filter(v -> {
			    throw new RuntimeException("forced failure");
		    })
		    .subscribe(ts);

		ts.assertNoValues()
		  .assertNotComplete()
		  .assertError(RuntimeException.class)
		  .assertErrorMessage("forced failure");
	}

	@Test
	public void syncFusion() {
		TestSubscriber<Object> ts = TestSubscriber.create();

		Flux.range(1, 10)
		    .filter(v -> (v & 1) == 0)
		    .subscribe(ts);

		ts.assertValues(2, 4, 6, 8, 10)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void asyncFusion() {
		TestSubscriber<Object> ts = TestSubscriber.create();

		UnicastProcessor<Integer> up =
				UnicastProcessor.create(new ConcurrentLinkedQueue<>());

		up.filter(v -> (v & 1) == 0)
		  .subscribe(ts);

		for (int i = 1; i < 11; i++) {
			up.onNext(i);
		}
		up.onComplete();

		ts.assertValues(2, 4, 6, 8, 10)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void asyncFusionBackpressured() {
		TestSubscriber<Object> ts = TestSubscriber.create(1);

		UnicastProcessor<Integer> up =
				UnicastProcessor.create(new ConcurrentLinkedQueue<>());

		Flux.just(1)
		    .hide()
		    .flatMap(w -> up.filter(v -> (v & 1) == 0))
		    .subscribe(ts);

		up.onNext(1);
		up.onNext(2);

		ts.assertValues(2)
		  .assertNoError()
		  .assertNotComplete();

		up.onComplete();

		ts.assertValues(2)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void asyncFusionBackpressured2() {
		TestSubscriber<Object> ts = TestSubscriber.create(1);

		UnicastProcessor<Integer> up =
				UnicastProcessor.create(new ConcurrentLinkedQueue<>());

		Flux.just(1)
		    .hide()
		    .flatMap(w -> up.filter(v -> (v & 1) == 0), false, 1, 1)
		    .subscribe(ts);

		up.onNext(1);
		up.onNext(2);

		ts.assertValues(2)
		  .assertNoError()
		  .assertNotComplete();

		up.onComplete();

		ts.assertValues(2)
		  .assertNoError()
		  .assertComplete();
	}

}
