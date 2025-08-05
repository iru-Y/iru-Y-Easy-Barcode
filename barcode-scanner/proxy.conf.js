require('dotenv').config();

const target = process.env.API_URL || 'http://localhost:8080';

module.exports = {
  '/barcode': {
    target,
    secure: false,
    changeOrigin: true,
  },
};
