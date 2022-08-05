--
-- PostgreSQL database dump
--

-- Dumped from database version 14.4
-- Dumped by pg_dump version 14.4

-- Started on 2022-08-06 07:34:04

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 212 (class 1255 OID 40966)
-- Name: refresh_updated(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.refresh_updated() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
	new.updated = now();
	return new;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 209 (class 1259 OID 40972)
-- Name: entity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.entity (
    id character varying NOT NULL,
    correlation_id character varying,
    created timestamp without time zone DEFAULT now() NOT NULL,
    updated timestamp without time zone DEFAULT now()
);


--
-- TOC entry 211 (class 1259 OID 49223)
-- Name: costing; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.costing (
    product_correlation_id character varying,
    product_valuation_type character varying,
    total_qty numeric,
    unit_cost numeric,
    total_cost numeric,
    valid_from timestamp without time zone,
    valid_to timestamp without time zone,
    isexpired boolean DEFAULT false NOT NULL
)
INHERITS (public.entity);


--
-- TOC entry 210 (class 1259 OID 40984)
-- Name: materialtransaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.materialtransaction (
    product_correlation_id character varying,
    product_valuation_type character varying,
    movement_type character varying,
    movement_qty numeric,
    acquisition_cost numeric,
    movement_date timestamp without time zone,
    costing_status character varying,
    costing_error_message character varying,
    movement_out_correlation_id character varying,
    customer_shipment_correlation_id character varying,
    iserror boolean DEFAULT false NOT NULL
)
INHERITS (public.entity);


--
-- TOC entry 3177 (class 2604 OID 49226)
-- Name: costing created; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.costing ALTER COLUMN created SET DEFAULT now();


--
-- TOC entry 3178 (class 2604 OID 49227)
-- Name: costing updated; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.costing ALTER COLUMN updated SET DEFAULT now();


--
-- TOC entry 3174 (class 2604 OID 40987)
-- Name: materialtransaction created; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materialtransaction ALTER COLUMN created SET DEFAULT now();


--
-- TOC entry 3175 (class 2604 OID 40988)
-- Name: materialtransaction updated; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materialtransaction ALTER COLUMN updated SET DEFAULT now();


--
-- TOC entry 3191 (class 2606 OID 49240)
-- Name: costing costing_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.costing
    ADD CONSTRAINT costing_pk PRIMARY KEY (id);


--
-- TOC entry 3185 (class 2606 OID 40980)
-- Name: entity entity_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity
    ADD CONSTRAINT entity_pk PRIMARY KEY (id);


--
-- TOC entry 3188 (class 2606 OID 49238)
-- Name: materialtransaction materialtransaction_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materialtransaction
    ADD CONSTRAINT materialtransaction_pk PRIMARY KEY (id);


--
-- TOC entry 3192 (class 1259 OID 49231)
-- Name: costing_product_correlation_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX costing_product_correlation_id_idx ON public.costing USING hash (product_correlation_id);


--
-- TOC entry 3180 (class 1259 OID 40994)
-- Name: entity_audit_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX entity_audit_idx ON public.entity USING btree (updated, created);


--
-- TOC entry 3181 (class 1259 OID 40981)
-- Name: entity_correlation_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX entity_correlation_id_idx ON public.entity USING hash (correlation_id);


--
-- TOC entry 3182 (class 1259 OID 40993)
-- Name: entity_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX entity_created_idx ON public.entity USING btree (created);


--
-- TOC entry 3183 (class 1259 OID 40982)
-- Name: entity_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX entity_id_idx ON public.entity USING hash (id);


--
-- TOC entry 3186 (class 1259 OID 40992)
-- Name: entity_updated_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX entity_updated_idx ON public.entity USING btree (updated);


--
-- TOC entry 3189 (class 1259 OID 40991)
-- Name: materialtransaction_product_correlation_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX materialtransaction_product_correlation_id_idx ON public.materialtransaction USING hash (product_correlation_id);


--
-- TOC entry 3195 (class 2620 OID 49235)
-- Name: costing costing_refresh_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER costing_refresh_updated BEFORE INSERT OR UPDATE ON public.costing FOR EACH ROW EXECUTE FUNCTION public.refresh_updated();


--
-- TOC entry 3194 (class 2620 OID 49236)
-- Name: materialtransaction materialtransaction_refresh_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER materialtransaction_refresh_updated BEFORE INSERT OR UPDATE ON public.materialtransaction FOR EACH ROW EXECUTE FUNCTION public.refresh_updated();


--
-- TOC entry 3193 (class 2620 OID 49232)
-- Name: entity refresh_updated; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER refresh_updated BEFORE INSERT OR UPDATE ON public.entity FOR EACH ROW EXECUTE FUNCTION public.refresh_updated();


-- Completed on 2022-08-06 07:34:06

--
-- PostgreSQL database dump complete
--

