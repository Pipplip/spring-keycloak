CREATE TABLE customers (
    id UUID PRIMARY KEY,
    keycloak_subject VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    street VARCHAR(255) NOT NULL,
    house_number VARCHAR(50) NOT NULL,
    postal_code VARCHAR(32) NOT NULL,
    city VARCHAR(255) NOT NULL,
    country_code VARCHAR(2) NOT NULL CHECK (country_code ~ '^[A-Z]{2}$'),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT customers_subject_not_blank CHECK (length(trim(keycloak_subject)) > 0),
    CONSTRAINT customers_email_not_blank CHECK (length(trim(email)) > 0)
);

CREATE TABLE container_types (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    volume_m3 NUMERIC(8, 2) NOT NULL CHECK (volume_m3 > 0),
    daily_price_cents BIGINT NOT NULL CHECK (daily_price_cents >= 0),
    currency_code VARCHAR(3) NOT NULL CHECK (currency_code ~ '^[A-Z]{3}$'),
    active BOOLEAN NOT NULL
);

CREATE TABLE containers (
    id UUID PRIMARY KEY,
    inventory_number VARCHAR(255) NOT NULL UNIQUE,
    container_type_id UUID NOT NULL REFERENCES container_types(id),
    status VARCHAR(32) NOT NULL CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'RETIRED')),
    CONSTRAINT containers_inventory_not_blank CHECK (length(trim(inventory_number)) > 0)
);
CREATE INDEX containers_container_type_id_idx ON containers(container_type_id);

CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    container_type_id UUID NOT NULL REFERENCES container_types(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    delivery_date DATE NOT NULL,
    pickup_date DATE NOT NULL,
    street VARCHAR(255) NOT NULL,
    house_number VARCHAR(50) NOT NULL,
    postal_code VARCHAR(32) NOT NULL,
    city VARCHAR(255) NOT NULL,
    country_code VARCHAR(2) NOT NULL CHECK (country_code ~ '^[A-Z]{2}$'),
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'DELIVERED', 'COMPLETED', 'CANCELLED')),
    daily_price_cents BIGINT NOT NULL CHECK (daily_price_cents >= 0),
    delivery_fee_cents BIGINT NOT NULL CHECK (delivery_fee_cents >= 0),
    currency_code VARCHAR(3) NOT NULL CHECK (currency_code ~ '^[A-Z]{3}$'),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL CHECK (version >= 0),
    CONSTRAINT bookings_valid_interval CHECK (pickup_date > delivery_date)
);
CREATE INDEX bookings_customer_id_idx ON bookings(customer_id);
CREATE INDEX bookings_container_type_id_idx ON bookings(container_type_id);
CREATE INDEX bookings_dates_idx ON bookings(delivery_date, pickup_date);

CREATE TABLE container_assignments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings(id),
    container_id UUID NOT NULL REFERENCES containers(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    released_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT assignments_release_after_assignment CHECK (released_at IS NULL OR released_at >= assigned_at)
);
CREATE INDEX container_assignments_booking_id_idx ON container_assignments(booking_id);
CREATE INDEX container_assignments_container_id_idx ON container_assignments(container_id);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id),
    invoice_number VARCHAR(255) NOT NULL UNIQUE,
    net_amount_cents BIGINT NOT NULL CHECK (net_amount_cents >= 0),
    tax_amount_cents BIGINT NOT NULL CHECK (tax_amount_cents >= 0),
    gross_amount_cents BIGINT NOT NULL CHECK (gross_amount_cents >= 0),
    currency_code VARCHAR(3) NOT NULL CHECK (currency_code ~ '^[A-Z]{3}$'),
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT', 'SENT', 'PAID', 'VOID')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT invoices_amounts_reconcile CHECK (gross_amount_cents = net_amount_cents + tax_amount_cents)
);

CREATE TABLE invoice_lines (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    description TEXT NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL CHECK (quantity > 0),
    unit_price_cents BIGINT NOT NULL CHECK (unit_price_cents >= 0),
    tax_rate NUMERIC(5, 4) NOT NULL CHECK (tax_rate >= 0 AND tax_rate <= 1),
    total_cents BIGINT NOT NULL CHECK (total_cents >= 0)
);
CREATE INDEX invoice_lines_invoice_id_idx ON invoice_lines(invoice_id);

CREATE FUNCTION prevent_overlapping_active_container_assignments()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    requested_period DATERANGE;
BEGIN
    IF NEW.released_at IS NOT NULL THEN
        RETURN NEW;
    END IF;

    PERFORM 1
    FROM containers
    WHERE id = NEW.container_id
    FOR UPDATE;

    SELECT daterange(delivery_date, pickup_date, '[)')
    INTO requested_period
    FROM bookings
    WHERE id = NEW.booking_id
      AND status <> 'CANCELLED';

    IF requested_period IS NULL THEN
        RETURN NEW;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM container_assignments existing_assignment
        JOIN bookings existing_booking ON existing_booking.id = existing_assignment.booking_id
        WHERE existing_assignment.container_id = NEW.container_id
          AND existing_assignment.released_at IS NULL
          AND existing_booking.status <> 'CANCELLED'
          AND existing_assignment.id <> NEW.id
          AND daterange(existing_booking.delivery_date, existing_booking.pickup_date, '[)') && requested_period
    ) THEN
        RAISE EXCEPTION 'Container % already has an overlapping active assignment', NEW.container_id
            USING ERRCODE = '23P01';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER container_assignments_no_active_overlap
BEFORE INSERT OR UPDATE OF booking_id, container_id, released_at
ON container_assignments
FOR EACH ROW
EXECUTE FUNCTION prevent_overlapping_active_container_assignments();
