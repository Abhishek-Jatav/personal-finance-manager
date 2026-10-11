# REST API (matches the assignment PDF)

Base path: /api. Login uses a session cookie. All routes except register and login need a valid session.

## Authentication
- POST /api/auth/register  body: username (email), password (min 8), fullName, phoneNumber (all required)
  201 {"message":"User registered successfully","userId":1} | 400 | 409
- POST /api/auth/login  body: username, password
  200 {"message":"Login successful"} + session cookie | 401
- POST /api/auth/logout  200 {"message":"Logout successful"} | 401

## Transactions
- POST /api/transactions  body: amount, date (YYYY-MM-DD, not future), category (name), description (optional)
  201 {"id","amount","date","category","description","type"} | 400 | 401
  The type (INCOME or EXPENSE) comes from the category. An unknown category is 400.
- GET /api/transactions  optional: startDate, endDate, categoryId, category (name), type
  200 {"transactions":[...]} newest first | 400 | 401
- PUT /api/transactions/{id}  body (all optional): amount, description, category (name). A date in the body is ignored; the date never changes.
  200 | 400 | 401 | 404
- DELETE /api/transactions/{id}  200 {"message":"Transaction deleted successfully"} | 401 | 404

## Categories
- GET /api/categories  200 {"categories":[{"name","type","isCustom"}]}
- POST /api/categories  body: name, type (INCOME or EXPENSE)  201 {"name","type","isCustom":true} | 400 | 409
  Names are unique per user, ignoring case, and cannot equal a default category name.
- DELETE /api/categories/{name}  200 {"message":"Category deleted successfully"}
  400 (used by a transaction) | 403 (default category) | 404

## Savings goals
- POST /api/goals  body: goalName, targetAmount, targetDate (future), startDate (optional, defaults to today)
  201 {"id","goalName","targetAmount","targetDate","startDate","currentProgress","progressPercentage","remainingAmount"}
- GET /api/goals  200 {"goals":[...]}
- GET /api/goals/{id}  200 | 400 | 403 (someone else's goal) | 404
- PUT /api/goals/{id}  body (all optional): targetAmount, targetDate, goalName  200 | 400 | 403 | 404
- DELETE /api/goals/{id}  200 {"message":"Goal deleted successfully"} | 403 | 404

currentProgress = income - expenses from startDate through today. It is never clamped, so it can be negative.
progressPercentage = currentProgress / targetAmount * 100 (2 decimals, HALF_UP). remainingAmount = targetAmount - currentProgress.

## Reports
- GET /api/reports/monthly/{year}/{month}
  {"month","year","totalIncome":{"Salary":3000.00},"totalExpenses":{"Food":400.00},"netSavings":1700.00}
- GET /api/reports/yearly/{year}
  {"year","totalIncome":{...},"totalExpenses":{...},"netSavings":...}

## Errors
{"timestamp","status","error","code","message","fieldErrors":[{"field","message"}],"path"}
400 invalid input, 401 not logged in, 403 forbidden, 404 not found, 409 conflict.
