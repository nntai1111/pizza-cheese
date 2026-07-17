/**
 * Load-test against local Spring + Docker seed data.
 * - GET /api/v1/cashier/orders (auth, 300k rows)
 * - GET /api/v1/pizzas (public catalog, small)
 *
 * Run from repo root:
 *   .\scripts\run-loadtest.ps1
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const LOGIN = __ENV.LOGIN || 'admin@gmail.com';
const PASSWORD = __ENV.PASSWORD || '123456';

const orderListDuration = new Trend('orders_list_duration', true);
const pizzasDuration = new Trend('pizzas_duration', true);
const errorRate = new Rate('api_errors');

export const options = {
  scenarios: {
    mixed_apis: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 10),
      duration: __ENV.DURATION || '20s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    orders_list_duration: ['p(95)<2000'],
    pizzas_duration: ['p(95)<1000'],
  },
};

export function setup() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ login: LOGIN, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  const ok = check(res, {
    'login status 200': (r) => r.status === 200,
    'login has token': (r) => !!(r.json('data.accessToken')),
  });

  if (!ok) {
    throw new Error(`Login failed: status=${res.status} body=${res.body}`);
  }

  return { token: res.json('data.accessToken') };
}

export default function (data) {
  // 1) Catalog — public, small data
  // const pizzasRes = http.get(`${BASE_URL}/api/v1/pizzas`, {
  //   headers: { Accept: 'application/json' },
  //   tags: { name: 'GET /pizzas' },
  // });
  // pizzasDuration.add(pizzasRes.timings.duration);
  // const pizzasOk = check(pizzasRes, {
  //   'pizzas status 200': (r) => r.status === 200,
  // });
  // errorRate.add(!pizzasOk);

  // 2) Cashier order list — auth, large table
  const page = Math.floor(Math.random() * 500);
  const size = 20;
  const ordersRes = http.get(
    `${BASE_URL}/api/v1/cashier/orders?page=${page}&size=${size}`,
    {
      headers: {
        Authorization: `Bearer ${data.token}`,
        Accept: 'application/json',
      },
      tags: { name: 'GET /cashier/orders' },
    },
  );
  orderListDuration.add(ordersRes.timings.duration);
  const ordersOk = check(ordersRes, {
    'orders status 200': (r) => r.status === 200,
    'orders has data': (r) => r.json('data') !== undefined,
  });
  errorRate.add(!ordersOk);

  sleep(0.3);
}

export function handleSummary(data) {
  const ordersP95 = data.metrics.orders_list_duration
    ? data.metrics.orders_list_duration.values['p(95)']
    : undefined;
  // const pizzasP95 = data.metrics.pizzas_duration
  //   ? data.metrics.pizzas_duration.values['p(95)']
  //   : undefined;

  console.log('--- summary ---');
  // console.log(`pizzas p95:       ${pizzasP95 !== undefined ? pizzasP95.toFixed(2) + ' ms' : 'n/a'}`);
  console.log(`orders_list p95:  ${ordersP95 !== undefined ? ordersP95.toFixed(2) + ' ms' : 'n/a'}`);
  console.log(
    `http failed:      ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`,
  );

  return { stdout: '' };
}
