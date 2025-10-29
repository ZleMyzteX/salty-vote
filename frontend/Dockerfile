# ---------- Build Stage ----------
FROM node:20 AS build

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# ---------- Runner Stage ----------
FROM node:20-alpine

WORKDIR /app

RUN apk add --no-cache curl && npm install -g serve

COPY --from=build /app/dist ./dist

EXPOSE 5173

CMD ["serve", "-s", "dist", "-l", "5173"]