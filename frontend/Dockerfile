# ---------- Build Stage ----------
FROM node:20 AS build

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

ENV VITE_API_BASE_URL=''

RUN npm run build

# ---------- Runner Stage ----------
FROM node:20-alpine

WORKDIR /app

RUN apk add --no-cache curl && npm install -g serve

COPY --from=build /app/build ./build

EXPOSE 5173

CMD ["serve", "-s", "build", "-l", "5173"]