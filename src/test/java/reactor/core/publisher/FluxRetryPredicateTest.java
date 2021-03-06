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

import org.junit.Test;
import reactor.test.TestSubscriber;

public class FluxRetryPredicateTest {

	final Flux<Integer> source = Flux.concat(Flux.range(1, 5),
			Flux.error(new RuntimeException("forced failure 0")));

	@Test(expected = NullPointerException.class)
	public void sourceNull() {
		new FluxRetryPredicate<>(null, e -> true);
	}

	@Test(expected = NullPointerException.class)
	public void predicateNull() {
		Flux.never()
		    .retry(null);
	}

	@Test
	public void normal() {
		int[] times = {1};

		TestSubscriber<Integer> ts = TestSubscriber.create();

		source.retry(e -> times[0]-- > 0)
		      .subscribe(ts);

		ts.assertValues(1, 2, 3, 4, 5, 1, 2, 3, 4, 5)
		  .assertError(RuntimeException.class)
		  .assertErrorMessage("forced failure 0")
		  .assertNotComplete();
	}

	@Test
	public void normalBackpressured() {
		int[] times = {1};

		TestSubscriber<Integer> ts = TestSubscriber.create(0);

		source.retry(e -> times[0]-- > 0)
		      .subscribe(ts);

		ts.assertNoValues()
		  .assertNoError()
		  .assertNotComplete();

		ts.request(2);

		ts.assertValues(1, 2)
		  .assertNoError()
		  .assertNotComplete();

		ts.request(5);

		ts.assertValues(1, 2, 3, 4, 5, 1, 2)
		  .assertNoError()
		  .assertNotComplete();

		ts.request(10);

		ts.assertValues(1, 2, 3, 4, 5, 1, 2, 3, 4, 5)
		  .assertError(RuntimeException.class)
		  .assertErrorMessage("forced failure 0")
		  .assertNotComplete();
	}

	@Test
	public void dontRepeat() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		source.retry(e -> false)
		      .subscribe(ts);

		ts.assertValues(1, 2, 3, 4, 5)
		  .assertError(RuntimeException.class)
		  .assertErrorMessage("forced failure 0")
		  .assertNotComplete();
	}

	@Test
	public void predicateThrows() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		source.retry(e -> {
			throw new RuntimeException("forced failure");
		})
		      .subscribe(ts);

		ts.assertValues(1, 2, 3, 4, 5)
		  .assertError(RuntimeException.class)
		  .assertErrorMessage("forced failure")
		  .assertNotComplete();
	}
}
