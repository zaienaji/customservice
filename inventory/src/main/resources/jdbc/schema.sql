-- public.materialtransaction definition

-- Drop table

-- DROP TABLE public.materialtransaction;

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
  customer_shipment_correlation_id varchar NULL,
  CONSTRAINT materialtransaction_pk PRIMARY KEY (id)
);
CREATE INDEX materialtransaction_correlation_id_idx ON public.materialtransaction USING hash (correlation_id);
CREATE INDEX materialtransaction_id_idx ON public.materialtransaction USING hash (id);
CREATE INDEX materialtransaction_product_correlation_id_idx ON public.materialtransaction USING hash (product_correlation_id);