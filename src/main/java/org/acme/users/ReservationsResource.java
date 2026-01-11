package org.acme.users;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.acme.users.model.Car;
import org.acme.users.model.Reservation;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * @author miller
 * @version 1.0.0
 * @since 2025/12/30
 */
@Path("/")
public class ReservationsResource {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(
                LocalDate startDate,
                LocalDate endDate,
                String name
        );

        public static native TemplateInstance listofreservations(Collection<Reservation> reservations);

        public static native TemplateInstance availablecars(Collection<Car> cars, LocalDate startDate, LocalDate endDate);
    }

    @Inject
    SecurityContext securityContext;
    @RestClient
    ReservationsClient reservationsClient;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(@RestQuery LocalDate startDate, @RestQuery LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().plusDays(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now().plusDays(7);
        }
        return Templates.index(startDate, endDate, securityContext.getUserPrincipal().getName());
    }

    @GET
    @Path("/get")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getReservations() {
        Collection<Reservation> reservations = reservationsClient.allReservations();
        return Templates.listofreservations(reservations);
    }

    @GET
    @Path("/available")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getAvailableCars(@RestQuery LocalDate startDate, @RestQuery LocalDate endDate) {
        Collection<Car> cars = reservationsClient.availability(startDate, endDate);
        return Templates.availablecars(cars, startDate, endDate);
    }

    @POST
    @Path("reserve")
    @Produces(MediaType.TEXT_HTML)
    public RestResponse<TemplateInstance> cretat(@RestForm LocalDate startDate,
                                                 @RestForm LocalDate endDate,
                                                 @RestForm Long carId) {
        Reservation reservation = new Reservation();
        reservation.carId = carId;
        reservation.startDay = startDate;
        reservation.endDay = endDate;
        reservationsClient.make(reservation);

        return RestResponse.ResponseBuilder
                .ok(getReservations())
                .header("HX-Trigger-After-Swap", "update-available-cars-list")
                .build();
    }
}
