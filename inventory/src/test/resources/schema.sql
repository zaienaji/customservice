DROP TABLE IF EXISTS public.materialtransaction;

CREATE TABLE public.materialtransaction (
  id varchar NOT NULL,
  correlation_id varchar NULL,
  product_correlation_id varchar NULL,
  product_valuation_type varchar NULL,
  movement_type varchar NULL,
  movement_qty numeric NULL,
  acquisition_cost numeric NULL,
  movement_date timestamp NULL,
  costing_status varchar NULL,
  costing_error_message varchar NULL,
  movement_out_correlation_id varchar NULL,
  customer_shipment_correlation_id varchar NULL
);
